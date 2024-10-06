# Contributing

Thank you for your interest in contributing to **TerminalTabTailor** !

I welcome contributions from everyone. Here are some guidelines to help you get started:

## Getting Started

1. **Fork the repository**: Click the "Fork" button at the top of this repository's GitHub page, and clone the forked repository to your machine.

   ```
   git clone https://github.com/your-username/TerminalTabTailor.git
   ```

2. **Set up the environment**:

    - Make sure you have IntelliJ IDEA 2024+ installed.
    - Install the IntelliJ Platform SDK: https://plugins.jetbrains.com/docs/intellij/setting-up-theme-environment.html#configuring-intellij-platform-plugin-sdk
    - Import the project into IntelliJ IDEA as a plugin project: https://plugins.jetbrains.com/docs/intellij/developing-plugins.html
3. **Create a new branch**:
   ```
   git checkout -b feature/my-new-feature
   ```

4. **Make your changes**: Implement your feature or fix the bug. Don't forget to add/update tests where applicable!


## Development Workflow

- **Code Style**

Follow IntelliJ's standard code style guidelines. Run `./gradlew check` before submitting your pull request to make sure your code passes all checks.

- **Testing**

Ensure all tests pass by running `./gradlew test`. If you add new functionality, please include tests to maintain coverage.

- **Documentation**

Update the README or other relevant docs if your changes affect the existing functionalities or usage.

## Submitting Changes

1. **Commit your changes**

Use descriptive and concise commit messages.

   ```
   git commit -m "Add my feature"
   ```

2. **Push your branch** to your forked repository:

   ```
   git push origin feature/my-new-feature
   ```

3. **Open a Pull Request**

Navigate to your fork on GitHub, and click the "Compare & pull request" button to open a new pull request. Please provide a clear description of what your PR does and why.

4. **Code Review**

Be prepared to receive feedback and make revisions based on reviewer comments.


## Issues and Bugs

- If you find a bug or have a feature request, please check the [Issues](https://github.com/romaindalichamp/TerminalTabTailor/issues) section first to see if someone else has already reported it.
- If it’s a new issue, feel free to [open one](https://github.com/romaindalichamp/TerminalTabTailor/issues/new). Provide as much detail as possible, including steps to reproduce the issue.

## Code of Conduct

Please note that we have a [Code of Conduct](CODE_OF_CONDUCT.md), and all participants are expected to adhere to it. Be respectful, professional, and constructive.