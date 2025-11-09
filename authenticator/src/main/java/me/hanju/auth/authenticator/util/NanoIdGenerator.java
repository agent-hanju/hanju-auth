package me.hanju.auth.authenticator.util;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

import lombok.experimental.UtilityClass;

/**
 * Nano ID 생성 유틸리티
 * URL-safe한 10자리 고유 ID 생성
 */
@UtilityClass
public class NanoIdGenerator {

  /** 기본 알파벳 (URL-safe) */
  private static final char[] DEFAULT_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
      .toCharArray();

  /** 기본 길이 (10자리) */
  private static final int DEFAULT_SIZE = 10;

  /**
   * 10자리 Nano ID 생성
   *
   * @return 10자리 Nano ID
   */
  public static String generate() {
    return NanoIdUtils.randomNanoId(NanoIdUtils.DEFAULT_NUMBER_GENERATOR, DEFAULT_ALPHABET, DEFAULT_SIZE);
  }

  /**
   * 커스텀 길이의 Nano ID 생성
   *
   * @param size 생성할 ID 길이
   * @return 지정된 길이의 Nano ID
   */
  public static String generate(int size) {
    return NanoIdUtils.randomNanoId(NanoIdUtils.DEFAULT_NUMBER_GENERATOR, DEFAULT_ALPHABET, size);
  }
}
