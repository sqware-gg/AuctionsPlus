# AuctionsPlus

**Join the SQWARE Discord: [discord.sqware.gg](https://discord.sqware.gg).**

AuctionsPlus is a Minecraft auction house plugin for Paper servers. It provides `/ah` GUI listings, fixed-price sales, timed bid auctions, Vault economy support, expiry returns, and claim mail.

Use it when you need item trading that is clear for players and manageable for staff.

## Features

- GUI auction browser.
- Fixed-price listings and timed bid auctions.
- Vault-backed buy, sell, bid, fees, and tax handling.
- Claim mail for bought, cancelled, expired, and returned items.
- Search, active listings, listing limits, and admin cancellation.
- LuckPerms/Vault rank-based active listing limit increases.
- Permission bypasses for listing limits and fees.
- Chat announcements for auction activity.
- API events for companion plugins such as DiscordPlus.

## Requirements

- Paper `26.2+`
- Java `25+`
- Vault
- A Vault-compatible economy plugin
- Maven wrapper included

## Commands

```text
/ah
/ah sell <price> [amount] [duration]
/ah auction <starting-bid> [amount] [duration]
/ah buy <id>
/ah bid <id> <amount>
/ah active
/ah claim
/ah search <text>
/ah cancel <id>
/ah help

/auctionsplus stats
/auctionsplus reload
/auctionsplus save
/auctionsplus cancel <id>
```

Aliases: `/auc`, `/auctionhouse`, `/auctions`, `/ahadmin`

## Permissions

```text
auctionsplus.use          - open the auction house
auctionsplus.sell         - create fixed-price listings
auctionsplus.auction      - create timed bid auctions
auctionsplus.buy          - buy listings
auctionsplus.bid          - bid on auctions
auctionsplus.cancel       - cancel own listings
auctionsplus.claim        - claim bought, cancelled, and expired items
auctionsplus.search       - search listings
auctionsplus.limit.bypass - bypass active listing limits
auctionsplus.fees.bypass  - bypass listing fees and sale tax
auctionsplus.notify       - receive auction announcements
auctionsplus.admin        - admin commands
```

## Build

```powershell
.\mvnw.cmd package
```

The jar is written to `target/AuctionsPlus-0.1.0.jar`.

## License

AuctionsPlus is licensed under the Apache License, Version 2.0.
