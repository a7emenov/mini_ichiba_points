package com.rakuten.market.points.api.impl.request

import com.rakuten.market.points.data.{Points, UserId}

private[impl] case class ChangePointsRequest(userId: UserId, amount: Points.Amount)
