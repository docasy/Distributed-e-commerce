# 多服务构建：通过 ARG SERVICE 指定要编译的模块
# 用法: docker build --build-arg SERVICE=ecommerce-user -t ecommerce-user .
FROM maven:3.8-openjdk-11 AS build
ARG SERVICE

WORKDIR /app

# 先复制父 POM 和公共模块（利用 Docker 缓存层）
COPY pom.xml .
COPY ecommerce-common/pom.xml ecommerce-common/
COPY ecommerce-common/src  ecommerce-common/src/

# 给父 POM 里声明但本次不编译的模块创建占位目录（Maven 要求所有模块目录存在）
RUN for mod in ecommerce-gateway ecommerce-user ecommerce-product ecommerce-order; do \
      if [ ! -d "$mod" ]; then mkdir -p "$mod/src/main/java" && \
        echo '<project><modelVersion>4.0.0</modelVersion><groupId>com.ecommerce</groupId><artifactId>'$mod'</artifactId><version>1.0.0</version><packaging>jar</packaging></project>' > "$mod/pom.xml"; fi; \
    done

# 复制目标服务的 POM 和源码（覆盖占位目录）
COPY ${SERVICE}/pom.xml ${SERVICE}/
COPY ${SERVICE}/src      ${SERVICE}/src/

# 只编译目标模块及其依赖 (ecommerce-common + parent)
RUN mvn package -DskipTests -pl ${SERVICE} -am -q

# 运行阶段：只放 JRE 和 jar，镜像尽量小
FROM eclipse-temurin:11-jre
ARG SERVICE
COPY --from=build /app/${SERVICE}/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
