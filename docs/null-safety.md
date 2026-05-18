# Building Null-Safety Spring Boot Applications

Kotlin’s built-in null safety is a standout feature, helping developers avoid the dreaded `NullPointerException` by enforcing strict rules on variable assignments. While Java has historically lacked this level of native protection, progress is being made. A promising future lies ahead with the [JEP draft: Null-Restricted and Nullable Types (Preview)](https://openjdk.org/jeps/8303099), which aims to bring similar capabilities to the language.

In the meantime, the Java ecosystem has developed several robust workarounds to tackle null safety:

* [Checker Framework](https://checkerframework.org/): A powerful analysis tool that uses annotations to detect potential null-safety issues during compilation.
* [JSR 305: Annotations for Software Defect Detection](https://jcp.org/en/jsr/detail?id=305#4): While technically dormant, its annotations for software defect detection remain a common industry staple.
* [JetBrains Annotations](https://www.jetbrains.com/help/idea/nullable-and-notnull-annotations.html): Highly popular among IntelliJ IDEA users for improving IDE-driven code analysis.
* [Jakarta Annotations](https://jakarta.ee/specifications/annotations/): The standard set of annotations provided by the Jakarta EE ecosystem.
* [JSpecify](https://jspecify.dev/): A modern collaborative effort to standardize null-safety annotations across the Java world, gaining significant traction in major frameworks.

Starting with version 7, the Spring ecosystem has fully embraced JSpecify, annotating its entire API surface for better null safety. When using an IDE like IntelliJ IDEA, this integration allows you to catch potential errors before they ever reach production. Furthermore, Kotlin developers benefit immensely, as these JSpecify annotations translate directly into Kotlin's native null-safety types.

To manage nullability in your own classes, you can use the `@Nullable`, `@NonNull`, and `@NullnessUnspecified` annotations from package `org.jspecify.annotations`:

```java
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NonNull;    
import org.jspecify.annotations.NullnessUnspecified;

public class Customer {
    @NonNull
    private String name;

    @Nullable
    private String email;

    @NullnessUnspecified
    private String phoneNumber;

    // method parameter and return type annoations
    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }
}
```

To reduce boilerplate, you can apply the `@NullMarked` annotation at the class or package level. This sets the default behavior for all members to non-null, meaning you only need to explicitly mark nullable fields:

```java
// ignore the imports
@NullMarked
public class Customer {
    private String name; // default to @NonNull

    @Nullable
    private String email;

    private String phoneNumber; // default to @NonNull

    // method parameter and return type annoations
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

Conversely, `@NullUnmarked` can be used to reverse this logic, making members nullable by default. In this scenario, you would only use `@NonNull` for specific fields that require a value:

```java
// ignore the imports
@NullUnmarked   
public class Customer {
    @NonNull
    private String name;

    private String email; // default to @Nullable

    @NonNull
    private String phoneNumber;

    // method parameter and return type annoations
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

IntelliJ IDEA highlights potential null-safety violations directly in the editor. For enforcing these rules during the build process, Gradle users can combine JSpecify with NullAway. For more details, see the official Spring blog post:  [Null Safety in Spring applications with JSpecify and NullAway](https://spring.io/blog/2025/03/10/null-safety-in-spring-apps-with-jspecify-and-null-away).

Maven users can simplify this integration by using the Nullability Maven Plugin, which streamlines the configuration for JSpecify and NullAway.

Add the following plugin configuration to your `pom.xml`:

```xml
<plugins>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.15.0</version>
    </plugin>
    <plugin>
        <groupId>am.ik.maven</groupId>
        <artifactId>nullability-maven-plugin</artifactId>
        <version>0.4.0</version>
        <extensions>true</extensions>
        <executions>
            <execution>
                <goals>
                    <goal>configure</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
    <...>
</plugins>
```

>[!NOTE]
>More details about nullability maven plugin, check [Making JSpecify/NullAway Easier for Maven Users with the Nullability Maven Plugin](https://ik.am/entries/900/en)

By adopting JSpecify annotations, you gain more predictable Java code and take advantage of advanced tooling in both your IDE and build pipeline. With Spring 7 leading the way, building reliable, null-safe applications is easier than ever, regardless of whether you choose Java or Kotlin.
