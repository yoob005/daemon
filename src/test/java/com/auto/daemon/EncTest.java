package com.auto.daemon;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

class EncTest {
	
	private static final String ALGORITHM = "AES";
	private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    // ��ȣȭ
    public static String encrypt(String data, String secretKey) throws Exception {
        SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    // ��ȣȭ
    public static String decrypt(String encryptedData, String secretKey) throws Exception {
        SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedBytes);
    }
    
    public static String enc(String encData) {
    	
    	String decData = "";
    	
        String secretKey = "1qaz@WSX3edc$RFV";

        String targetText = "sNit1KIcbOkgARKxsHnWvho2Ow6jF4y3CpAxmuEx";

        // Using Jasypt
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(secretKey);
        encryptor.setAlgorithm("PBEWithMD5AndDES"); //Default

        String encryptedText = encryptor.encrypt(targetText);
        System.out.println("encryptedText = " + encryptedText);

        String decryptedText = encryptor.decrypt(encryptedText);
        System.out.println("decryptedText = " + decryptedText);
    	
    	return decData;
    }
	
    public static void main(String[] args) throws Exception {
        String originalData = "aPbh0OFqAroydF9qIQvwrSDBc8rizbXCJxfuYZXG"; // ��ȣȭ�� ��������
        String encrypted = encrypt(originalData, "1qaz@WSX3edc$RFV");
//        System.out.println("Encrypted: " + encrypted);
        String decrypted = decrypt(encrypted, "1qaz@WSX3edc$RFV");
//        System.out.println("Decrypted: " + decrypted);
        enc("aaa");
	}

	
}
