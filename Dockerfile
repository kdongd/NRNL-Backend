# ===== 1단계: 빌드 =====
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle
COPY src ./src

# 테스트는 GitHub Actions(CI)에서 이미 돌리므로 이미지 빌드에서는 생략한다.
RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

# ===== 2단계: 실행 =====
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

# 타임존 정책: 한국 사용자 전용 서비스라 서버 기준 시간을 Asia/Seoul로 고정한다.
# DB(timestamp)와 API(LocalDateTime)에 타임존 정보가 없고, 클라이언트는 한국 시각을 보낸다.
# 서버가 UTC면 9시간 어긋나서 @PastOrPresent가 운동 기록을 거부하고 룰 엔진의 "오늘"이 틀어진다.
# 해외 사용자를 받게 되면 timestamptz + 사용자별 타임존으로 바꿔야 한다.
#
# 운영 프로필(prod): application-prod.yml을 적용한다. OPENAI_API_KEY가 없으면 앱이 뜨지 않는다.
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
