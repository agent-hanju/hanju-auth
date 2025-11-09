package me.hanju.auth.validator.enums;

import java.util.function.BiFunction;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** JWT 토큰 타입 열거형 */
@Getter
@RequiredArgsConstructor
public enum JwtTokenType {
  ACCESS_TOKEN((expireMinutes, multiplier) -> expireMinutes),
  REFRESH_TOKEN((expireMinutes, multiplier) -> expireMinutes * multiplier),
  REFRESH_TOKEN_LONG((expireMinutes, multiplier) -> expireMinutes * (long) Math.pow(multiplier, 2));

  private final BiFunction<Long, Long, Long> expireMinutesCalculator;

  /**
   * 토큰 만료 시간(분) 계산
   *
   * @param baseMinutes
   * @param multiplier
   * @return
   */
  public Long calculateExpireMinutes(final Long baseMinutes, final Long multiplier) {
    return expireMinutesCalculator.apply(baseMinutes, multiplier);
  }
}
