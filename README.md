# 🤖 MyManus

1. Designed to address programmers' daily challenges
2. A personal toolbox and assistant for developers
3. Compact, practical, and enjoyable
4. Open to innovative ideas

## 📹 Video

Task: Report China's top 10 cities by GDP:

[![MyManus](https://img.youtube.com/vi/G3EZpnW1tdM/0.jpg)](https://youtu.be/G3EZpnW1tdM)

## 🎯 Perform tasks

When you run the application, it will ask you to enter a task, here are some examples:

1. What is the capital of France?
2. Write python code to print the Fibonacci sequence

And a more complex task:

```markdown
Report China's top 10 cities by GDP including:

* Nominal GDP
* Population
* GDP per capita
* Major industries

Additional requirements

* Provide analysis of the results
* Draw a bar chart and save it in PNG format

:end

```

## 🚀 How to Run

### 📋 Prerequisites

1. JDK 17+
2. Install npx globally using npm:
    ```shell
    npm install -g npx
    ```
3. Set required KEYs: copy [application-private-template.yml](src/main/resources/application-private-template.yml)
   to `application-private.yml` and fill in your own keys.

* Click <a href="https://platform.deepseek.com/api_keys" target="_blank">DeepSeek key</a>
  to register to get your deepseek key
* Click <a href="https://serpapi.com/users/sign_in" target="_blank">SerpApi key</a> to register to get free
  tokens for each month.

### 🛠️ Run with IDE

Open the project in your IDE.

Open `MyManusApplication` in the editor and click `run`.

### 🛠️ Run with Maven

```shell
  ./mvnw spring-boot:run
```
