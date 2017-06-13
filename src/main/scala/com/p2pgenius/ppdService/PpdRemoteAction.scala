package com.p2pgenius.ppdService

import com.p2pgenius.ppdService.PpdRemoteActionType.PpdRemoteActionType

/**
  * Created by joe on 2017/6/13.
  */
case class PpdRemoteAction(action: PpdRemoteActionType, body: Any = null)

object PpdRemoteActionType extends Enumeration{
  type PpdRemoteActionType = Value
  val ASK_LOAN_LIST_STATUS,
      ASK_LOAN_LIST_INFO,
      BID,
      LOAN_BIDER,
      FETCH_HTML_INFO
    = Value
}