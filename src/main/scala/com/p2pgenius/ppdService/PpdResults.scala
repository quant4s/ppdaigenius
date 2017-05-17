package com.p2pgenius.ppdService


case class LoanListResult(LoanInfos: List[LoanList], Result: Int, ResultMessage: String, ResultCode: String)
case class LoanList(ListingId: Long, Title: String, CreditCode: String, Rate: Double, Amount: Double, Months: Double, PayWay: Int, RemainFunding:Double )

case class LoanInfoResult(LoanInfos: List[LoanInfo], Result: Int, ResultMessage: String, ResultCode: String)
case class LoanInfo(FistBidTime: String, LastBidTime: String, LenderCount: Int, AuditingTime: String, RemainFunding: Int,
                    DeadLineTimeOrRemindTimeStr: String, CreditCode: String, ListingId: Long, Amount: Int, Months: Int,
                    CurrentRate: Double, BorrowName: String, Gender: Int, EducationDegree: String, GraduateSchool: String,
                    StudyStyle: String, Age: Int, SuccessCount: Int, WasteCount: Int, CancelCount: Int, FailedCount: Int,
                    NormalCount: Int, OverdueLessCount: Int, OverdueMoreCount: Int, OwingPrincipal: Int, OwingAmount: Int,
                    AmountToReceive: Int, FirstSuccessBorrowTime: String, RegisterTime: String, CertificateValidate: Int,
                    NciicIdentityCheck:Int, PhoneValidate: Int, VideoValidate: Int, CreditValidate: Int, EducateValidate: Int,
                    sumLoanAmount:Double = 0, maxLoanAmount: Double = 0, highestDebt: Double = 0,
                    status: Int = 2)


case class LoanStatusResult(Infos: List[LoanStatus], Result: Int, ResultMessage: String, ResultCode: String)
case class LoanStatus(ListingId: Long, Status: Int)


case class BidResult(ListingId: Int, Amount: Int, ParticipationAmount: Int, Result: Int, ResultMessage: String)

case class QueryBalanceResult(Balance:List[Balance], Result: Int, ResultMessage: String)
case class Balance(AccountCategory: String, Balance: Double)

case class UserInfoResult(UserName: String, ReturnMessage: String, ReturnCode: String)


