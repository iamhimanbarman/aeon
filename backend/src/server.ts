import { env } from "./config/env.js";
import { buildApp } from "./app.js";

const app = buildApp();

app
  .listen({
    host: env.HOST,
    port: env.PORT
  })
  .catch(async (error) => {
    app.log.error(error, "failed_to_start");
    await app.close();
    process.exit(1);
  });
