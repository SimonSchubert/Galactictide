# Galactictide Bot

A Kotlin-based bot for interacting with the GalaSwap DEX. This bot can be used to automate trading strategies, fetch asset balances, and execute swaps.

## Features

*   Fetches user asset balances.
*   Retrieves the best quote across multiple fee tiers.
*   Executes swap transactions.
*   Monitors transaction status.
*   Configurable via `config.json` for token pairs and trading intervals.

## Setup

1.  **Dependencies:** Ensure you have the necessary dependencies installed.
2.  **Configuration:**
    *   Create a `.env` file in the root directory with the following:
        ```
        PRIVATE_KEY=your_private_key_here
        USER_ADDRESS=your_user_address_here
        ```
    *   Create a `config.json` file in the root directory with your desired trading configuration:
        ```json
        {
          "tokenIn": "token_symbol_in",
          "tokenOut": "token_symbol_out",
          "intervalsLeft": 10,
          "totalIntervals": 10
        }
        ```
        (Replace `token_symbol_in` and `token_symbol_out` with actual token contract addresses or symbols as required by the bot, e.g., "GALA[ethereum]")

## Usage

Run the `Main.kt` file to start the bot.

*Disclaimer: This bot interacts with a decentralized exchange and involves financial transactions. Use at your own risk. Ensure your private key is kept secure.*
