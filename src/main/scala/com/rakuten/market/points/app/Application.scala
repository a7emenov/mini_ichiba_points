package com.rakuten.market.points.app


import cats.effect.ExitCode
import com.rakuten.market.points.api.core.Api
import com.rakuten.market.points.settings.{ApplicationSettings, ServerSettings}
import com.rakuten.market.points.storage.core.PointsStorage
import com.rakuten.market.points.storage.impl.PostgresContext
import io.getquill.context.monix.Runner
import io.getquill.{PostgresMonixJdbcContext, SnakeCase}
import monix.eval.{Task, TaskApp}
import org.flywaydb.core.Flyway
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import pureconfig.generic.ProductHint
import pureconfig.{CamelCase, ConfigFieldMapping}
import cats.syntax.flatMap._
import com.rakuten.market.points.service.core.PointsService

import scala.concurrent.duration._

object Application extends TaskApp {

  private lazy implicit val dbCtx: PostgresContext =
    new PostgresMonixJdbcContext(SnakeCase, "db", Runner.default)

  override def run(args: List[String]): Task[ExitCode] =
    for {
      settings <- appSettings
      _ <- migrateDatabase
      pointsStorage = PointsStorage.postgres
      service = PointsService.default(settings.points.transaction, pointsStorage)
      _ <- startScheduledTasks(settings, service)
      api = Api.points("", settings.api.cors, settings.api.auth, service)
      exitCode <- runServer(api, settings.api.server)
    } yield exitCode

  private def migrateDatabase: Task[Unit] =
    Task.delay {
      Flyway
        .configure()
        .dataSource(dbCtx.dataSource)
        .load()
        .migrate()
    }

  private def runServer(api: Api[Task], settings: ServerSettings): Task[ExitCode] =
    BlazeServerBuilder[Task]
      .bindHttp(settings.port, settings.host)
      .withHttpApp(Router(api.root -> api.routes).orNotFound)
      .serve
      .compile
      .lastOrError

  private def appSettings: Task[ApplicationSettings] = {
    import com.rakuten.market.points.settings.AuthSettings.signingKeyReader
    import pureconfig.generic.auto._

    implicit def hint[T]: ProductHint[T] = ProductHint(ConfigFieldMapping(CamelCase, CamelCase))
    Task.delay(pureconfig.loadConfigOrThrow[ApplicationSettings])
  }

  private def startScheduledTasks(settings: ApplicationSettings,
                                  service: PointsService[Task]): Task[Unit] = {
    val start =
      Task.sleep(10.seconds) >>
      Task.gatherUnordered(
        List(
          Schedule.removeExpiredTransactions(settings.points.transaction, service),
          Schedule.removeExpiredPoints(settings.points.expiring, service)
        )
      )
    start.forkAndForget
  }
}
