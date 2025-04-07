# MyManus

MyManus is your AI assistant—simply speak your task, and let it get the job done.

## How to Run

### Prerequisites

1. JDK 17+
2. Install npx globally using npm:
```shell
npm install -g npx
```

3. For Windows users, you need to modify the `mcp-servers-config.json` file in the resources directory, changing `npx`
   to `npx.cmd`.

4. Set required KEYs: copy [application-private-template.yml](src/main/resources/application-private-template.yml) 
   to `application-private.yml` and fill in your own keys.

* Click <a href="https://platform.deepseek.com/api_keys" target="_blank">DeepSeek key</a>
   to register to get your deepseek key
* Click <a href="https://serpapi.com/users/sign_in" target="_blank">SerpApi key</a> to register to get free
   tokens for each month.

### Run with IDE

Open the project in your IDE.

Open `MyManusApplication` in the editor and click `run`.

### Run with Maven

```shell
./mvnw spring-boot:run
```

### Perform tasks

When you run the application, it will ask you to enter a task, here are some examples:

1. What is the capital of France?
2. Write python code to print the Fibonacci sequence

And more complex tasks:

```markdown
Please report the **top 10 cities in China by GDP**, including the following details:

- **Nominal GDP**
- **Population**
- **GDP per capita**
- **Key industries**

**Additional tasks:**
- **Analyze and explain the results**
- **Generate a bar chart and save it as a PNG file**

---  
### **Key Notes:**
1. **Data Scope:** Focus on the latest available year (2023 or 2024).
2. **Analysis:** Highlight trends (e.g., growth drivers, regional disparities).
3. **Visualization:** Ensure the chart is labeled (city names, GDP values) and formatted clearly.

```
