# @capire/xtravels-java

A travel booking application built with CAP Java using master data provided by [capire/xflights-java](https://github.com/capire/xflights-java).

## Get it

```sh
git clone https://github.com/capire/xtravels-java
cd xtravels-java
mvn package
```

The package has dependencies to other `capire` packages, including [_`sap.capire/xflights-data`_](https://github.com/capire/xflights-java/packages/2693214), that can be pulled from [GitHub Packages](#using-github-packages) or from a [local workspace setup](#using-workspaces) as follows...

### Using GitHub Packages

Reuse packages among the *[capire samples](https://github.com/capire)* are published to the [GitHub Packages](https://docs.github.com/packages) registry. This includes Maven packages as well as NPM packages.

#### Authenticate for Maven

Authenticate to GitHub's Maven repository by adding the following to your `~/.m2/settings.xml`:

```xml
<servers>
  <!-- ... -->
  <server>
    <id>github</id>
    <username>USERNAME</username>
    <password>TOKEN</password>
  </server>
</servers>
```

As password you're using a Personal Access Token (classic) with `read:packages` scope.
Read more about it in [Authenticating to GitHub Packages](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry).

#### Authenticate for NPM

Run `npm login` once like that:

```sh
npm login --scope=@capire --registry=https://npm.pkg.github.com
```

When prompted for a password enter a Personal Access Token (classic) with `read:packages` scope.
Learn more about that in [Authenticating to GitHub Packages](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-npm-registry#authenticating-to-github-packages).

A successfull `npm login` adds entries like that to your local `~/.npmrc` file, which allow you to npm install @capire packages subsequently using `npm add` or `npm install` as usual:

```properties
@capire:registry=https://npm.pkg.github.com/
//npm.pkg.github.com/:_authToken=<generated token>
```

### Using Workspaces

Alternatively you can work with related packages in local workspace setups like so:

1. Create a workspace root for NPM, e.g. at `cap/samples-java`:
   ```sh
   mkdir -p cap/samples-java && cd cap/samples-java
   echo '{"workspaces":["*","*/apis/*"]}' > package.json
   ```

2. Add related projects:
   ```sh
   git clone https://github.com/capire/xtravels-java
   git clone https://github.com/capire/xflights-java
   git clone https://github.com/capire/common
   ```

3. Install NPM dependencies:
   ```sh
   npm install
   ```

4. Install Maven dependencies:
   ```sh
   cd xflights-java && mvn install
   ```

For Maven, this will share cross dependencies between projects via your local Maven repository placed at `~/.m2`.
For NPM, this will install all dependencies of all cloned projects, with cross dependencies between them being *symlinked* automatically by `npm install`.

## Run it

To start the `xtravels` application, run the `Application` class in your preferred IDE.
Alternatively, execute `mvn spring-boot:run` on the command line.

Click the http://localhost:8080 link in the terminal to open the app in a browser.

The dependencies to the [xflights](https://github.com/capire/xflights-java) application are mocked automatically by CAP.

### With xflights

You can also connect the `xtravels` application to a locally running [xflights](https://github.com/capire/xflights-java) application.

Start the `xflights` application first, then start the `xtravels` application with Spring Boot profile `hybrid`.

## License

Copyright (c) 2025 SAP SE or an SAP affiliate company. All rights reserved. This file is licensed under the Apache Software License, version 2.0 except as noted otherwise in the [LICENSE](LICENSE) file.
