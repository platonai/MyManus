# Native Image Building

Awesome! Here's a **complete `pom.xml` setup** for a Spring Boot 3+ project that:

✅ Packages your app  
✅ Builds a container image  
✅ Supports **GraalVM native image**  
✅ Uses a **`native` Maven profile**  
✅ Pushes to **DockerHub** (optional)

---

### ✅ `pom.xml` Example with Native Image Profile

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>demo-app</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>jar</packaging>
    <name>Spring Native Demo</name>

    <properties>
        <java.version>17</java.version>
        <spring-boot.version>3.2.2</spring-boot.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <!-- Add more dependencies as needed -->
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Plugin with Image Builder -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
                <configuration>
                    <image>
                        <name>yourdockerhubusername/demo-app</name>
                        <builder>paketobuildpacks/builder:tiny</builder>
                        <env>
                            <BP_NATIVE_IMAGE>true</BP_NATIVE_IMAGE>
                        </env>
                    </image>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <!-- Native Profile -->
    <profiles>
        <profile>
            <id>native</id>
            <properties>
                <spring-boot.build-image.pullPolicy>IF_NOT_PRESENT</spring-boot.build-image.pullPolicy>
            </properties>
        </profile>
    </profiles>
</project>
```

---

### 🛠️ How to Use

#### 👉 Build normal JVM image:
```shell
mvn spring-boot:build-image
```

#### 👉 Build native image:
```shell
mvn spring-boot:build-image -Pnative
```

Make sure:
- You have **GraalVM installed** (17 or 21)
- You ran: `gu install native-image`

---

### 🐳 Push to DockerHub (optional)

To push the image:

```bash
docker login
docker push yourdockerhubusername/demo-app
```

You can then pull it from any environment or use in Kubernetes.

---

Would you like a **GitHub Actions CI/CD pipeline** for this too? Or an AWS ECR version?
