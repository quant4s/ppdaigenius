package com.p2pgenius.strategies

/**
  * Created by joe on 2017/5/12.
  */
case class StrategyDesc(
                         ageMin: Int, ageMax: Int, // 年龄
                         male:Boolean, female:Boolean, // 性别
                         monthMin: Int, monthMax: Int, //期限范围
                         rateMin: Int, rateMax: Int, // 利率范围
                         amountMin: Int, amountMax: Int, // 借款金额范围
                         aaa:Boolean, aa:Boolean, a:Boolean, b:Boolean, c:Boolean, d:Boolean, e:Boolean, f:Boolean, // 魔镜等级
                         first: Boolean, app: Boolean, quick: Boolean, // 排除标题栏

                         validateStyle: Boolean, // 认证条件 false: 满足任意认证  true: 满足所有认证
                         videoValidate: Boolean, phoneValidate: Boolean, creditValidate: Boolean, // 认证项目
                         certificateValidate: Boolean, educateValidate: Boolean, nciicIdentityCheck: Boolean,

                         educationDegreeQT: Boolean, educationDegreeBS: Boolean, educationDegreeYJS: Boolean, educationDegreeBK: Boolean, //学历类别
                         educationDegreeZSB: Boolean, educationDegreeDZ: Boolean, educationDegreeZK: Boolean,
                         studyStyleQT: Boolean, studyStylePT: Boolean, studyStyleZK: Boolean, studyStyleCR: Boolean,
                         school985: Boolean, school211: Boolean, schoolB1: Boolean, schoolB2: Boolean, schoolB3: Boolean, // 学校分类
                         successCountMin: Int, successCountMax: Int, // 成功借款次数
                         wasteCountMin:Int, wasteCountMax:Int, //流标次数
                         cancelCountMin:Int, cancelCountMax:Int, //撤标次数
                         failCountMin:Int, failCountMax:Int, // 失败次数
                         normalCountMin:Int, normalCountMax:Int, // 正常还清次数
                         overdueLessCountMin: Int, overdueLessCountMax: Int, // 逾期(1-15)还清次数
                         overdueMoreCountMin:Int, overdueMoreCountMax:Int, // 逾期(15天以上)还清次数
                         owingAmountMin: Int, owingAmountMax: Int // 待还金额

                       )  
