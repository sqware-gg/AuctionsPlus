# Contributing

Use small, focused changes and keep behavior configurable for production servers.

## Development

- Target Paper `26.1.2`.
- Use Java `25`.
- Build with `./mvnw package` or `.\mvnw.cmd package`.
- Keep player data migrations backward compatible.
- Avoid blocking network or disk work on hot player events.

## Pull Requests

- Explain the behavior change.
- Include any new config keys in `src/main/resources/config.yml`.
- Update `README.md` when commands, permissions, placeholders, or data format changes.
- Run the Maven build before submitting.
