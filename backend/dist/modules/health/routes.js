export async function registerHealthRoutes(app) {
    app.get("/health/live", async () => ({
        status: "ok",
        service: "aeon-backend"
    }));
    app.get("/health/ready", async () => {
        await app.db `select 1`;
        return {
            status: "ready",
            service: "aeon-backend"
        };
    });
}
