# ===== 1단계: 빌드 =====
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle
COPY src ./src

# 테스트는 CI에서 이미 돌리므로 이미지 빌드에서는 생략.
RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

# ===== 2단계: 실행 =====
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

# 타임존 정책 : 현재는 한국 사용자 전용 서비스라 서버 기준 시간을 Asia/Seoul로 고정했습니다.
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
