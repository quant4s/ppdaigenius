package com.ppdai.open

import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.pkcs.RSAPrivateKeyStructure
import sun.misc.BASE64Decoder
import sun.misc.BASE64Encoder
import javax.crypto.Cipher
import java.security._
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPrivateKeySpec
import java.security.spec.X509EncodedKeySpec

import com.ppdai.open.PKCSType.PKCSType


/**
  * Created by joe on 2017/4/20.
  */
class RsaCryptoHelper(val pkcsTyps: PKCSType, publicKey: String, privateKey: String) {

  val KEY_ALGORTHM = "RSA" //
  val SIGNATURE_ALGORITHM = "SHA1withRSA"

  private val keyFactory = KeyFactory.getInstance(KEY_ALGORTHM)

  //初始化公钥
  val pubKeyBytes = decryptBASE64(publicKey)
  val x509EncodedKeySpec = new X509EncodedKeySpec(pubKeyBytes)
  val _publicKey = keyFactory.generatePublic(x509EncodedKeySpec)

  //初始化私钥
  val keyBytes = decryptBASE64(privateKey)
  val pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytes)
  val _privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec)


  /**
    * BASE64解密
    *
    * @param key
    * @return
    * @throws Exception
    */
  @throws[Exception]
  private def decryptBASE64(key: String) = (new BASE64Decoder).decodeBuffer(key)

  /**
    * BASE64加密
    *
    * @param key
    * @return
    * @throws Exception
    */
  @throws[Exception]
  private def encryptBASE64(key: Array[Byte]) = (new BASE64Encoder).encodeBuffer(key)

  /**
    * 用私钥解密 * @param data    加密数据
    *
    * @return
    * @throws Exception
    */
  @throws[Exception]
  def decryptByPrivateKey(data: Array[Byte]): Array[Byte] = {
    //对数据解密
    val cipher = Cipher.getInstance(keyFactory.getAlgorithm)
    cipher.init(Cipher.DECRYPT_MODE, _privateKey)
    cipher.doFinal(data)
  }

  /**
    * 用私钥解密
    *
    * @param data 加密数据
    * @return
    * @throws Exception
    */
  def decryptByPrivateKey(data: String): String = new String(decryptByPrivateKey(decryptBASE64(data)))

  /**
    * 用公钥加密
    *
    * @param data 加密数据
    * @return
    * @throws Exception
    */
  @throws[Exception]
  def encryptByPublicKey(data: Array[Byte]): Array[Byte] = {
    //对数据解密
    val cipher = Cipher.getInstance(keyFactory.getAlgorithm)
    cipher.init(Cipher.ENCRYPT_MODE, _publicKey)
    cipher.doFinal(data)
  }

  /**
    * 用公钥加密
    *
    * @param data 加密数据
    * @return
    * @throws Exception
    */
  @throws[Exception]
  def encryptByPublicKey(data: String): String = encryptBASE64(encryptByPublicKey(data.getBytes))

  /**
    * 用私钥对信息生成数字签名
    *
    * @param data //加密数据
    * @return
    * @throws Exception
    */
  @throws[Exception]
  def sign(data: Array[Byte]): String = {
    //用私钥对信息生成数字签名
    val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
    signature.initSign(_privateKey)
    signature.update(data)
    encryptBASE64(signature.sign)
  }

  /**
    * 用私钥对信息生成数字签名
    *
    * @param data //加密数据
    * @return
    * @throws Exception
    */
  @throws[Exception]
  def sign(data: String): String = sign(data.getBytes)

  /**
    * 校验数字签名
    *
    * @param data 加密数据
    * @param sign 数字签名
    * @return
    * @throws Exception
    */
  @throws[Exception]
  def verify(data: Array[Byte], sign: String): Boolean = {
    val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
    signature.initVerify(_publicKey)
    signature.update(data)
    //验证签名是否正常
    signature.verify(decryptBASE64(sign))
  }

  /**
    * 校验数字签名
    *
    * @param data 加密数据
    * @param sign 数字签名
    * @return
    * @throws Exception
    */
  @throws[Exception]
  def verify(data: String, sign: String): Boolean = {
    return verify(data.getBytes, sign)
  }
}
