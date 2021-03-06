# Ivana Chess - Matchmaker

Matchmaker.

## How to build

```bash
./gradlew :ivana-chess-matchmaker:assemble
```

## How to test

```bash
./gradlew :ivana-chess-matchmaker:check
```

## How to run

```bash
./gradlew :ivana-chess-matchmaker:bootRun
```

By default, the application will run with `dev` profile, but you can override it with
property `ivana-chess-matchmaker.profile`
.

Available profiles:

- `dev`: used to local development.

## Configuration

You can override configuration by setting JVM properties or environment variables.

|                Property                |          Environment variable          |                    Description                    |        Default value       |
|:--------------------------------------:|:--------------------------------------:|:-------------------------------------------------:|:--------------------------:|
|         ivana-chess.broker.host        |         IVANA_CHESS_BROKER_HOST        |                   Host of broker                  |          127.0.0.1         |
|         ivana-chess.broker.port        |         IVANA_CHESS_BROKER_PORT        |                   Port of broker                  |            5672            |
|        ivana-chess.broker.vhost        |        IVANA_CHESS_BROKER_VHOST        |              Virtual host to connect              |              /             |
|       ivana-chess.broker.username      |       IVANA_CHESS_BROKER_USERNAME      |         Username used to connect to broker        |            guest           |
|       ivana-chess.broker.password      |       IVANA_CHESS_BROKER_PASSWORD      |         Password used to connect to broker        |            guest           |
|     ivana-chess.broker.instance-id     |     IVANA_CHESS_BROKER_INSTANCE_ID     |     ID used to create instance-specific queue     | ivana-chess-matchmaking-01 |
|     ivana-chess.broker.match-queue     |     IVANA_CHESS_BROKER_MATCH_QUEUE     |                Name of match queue                |            match           |
|  ivana-chess.broker.matchmaking-queue  |  IVANA_CHESS_BROKER_MATCHMAKING_QUEUE  |             Name of matchmaking queue             |         matchmaking        |
|     ivana-chess.broker.ssl.enabled     |     IVANA_CHESS_BROKER_SSL_ENABLED     |      If SSL is enabled for broker connection      |            false           |
| ivana-chess.broker.ssl.verify-hostname | IVANA_CHESS_BROKER_SSL_VERIFY_HOSTNAME | If certificate hostname is verified on connection |            false           |
|       ivana-chess.logging.config       |       IVANA_CHESS_LOGGING_CONFIG       |         Path to Logback configuration file        |    classpath:logback.xml   |
