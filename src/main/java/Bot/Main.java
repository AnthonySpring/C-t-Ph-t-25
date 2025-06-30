package Bot;

import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;

import java.io.IOException;

public class Main {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "1491";
    private static final String PLAYER_NAME = "Basubeso";
    private static final String SECRET_KEY = "sk-4UF2Dx9QRU6-TVn83rTFIg:P86wA52x9cG_M5hPaimdt6LWkVoD-uj-55hDfbQsCGUGjIac7walqZLjFCnhZoQeOcJKqGRXOACGr6DUWbzTpA";

    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        Emitter.Listener onMapUpdate = new ProGamerBotListener(hero);
        hero.setOnMapUpdate(onMapUpdate);
        hero.start(SERVER_URL);
    }
}