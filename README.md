# scala-vault [![license](http://img.shields.io/:license-MIT-blue.svg)](LICENSE)
[![Scaladoc](http://javadoc-badge.appspot.com/pt.tecnico.dsi/scala-vault_2.13.svg?label=scaladoc&style=plastic&maxAge=604800)](https://ist-dsi.github.io/scala-vault/api/latest/pt/tecnico/dsi/vault/index.html)
[![Latest version](https://index.scala-lang.org/ist-dsi/scala-vault/scala-vault/latest.svg)](https://index.scala-lang.org/ist-dsi/scala-vault/scala-vault)

[![Build Status](https://travis-ci.org/ist-dsi/scala-vault.svg?branch=master&style=plastic&maxAge=604800)](https://travis-ci.org/ist-dsi/scala-vault)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/f96a28fe69964e498a9dd711a3416b11)](https://www.codacy.com/app/IST-DSI/scala-vault?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ist-dsi/scala-vault&amp;utm_campaign=Badge_Grade)
[![BCH compliance](https://bettercodehub.com/edge/badge/ist-dsi/scala-vault)](https://bettercodehub.com/results/ist-dsi/scala-vault)

The Scala client for HashiCorp's Vault.

Currently supported endpoints:
  
- Auth Methods:
  - Token
  - AppRole
- Secret Engines:
  - KeyValue version 1
  - PKI
  - Consul
  - Elasticsearch
  - MongoDB
  - MySQL
- Sys:
  - Init
  - Health
  - Leader (status and step down)
  - Seal (status, seal, and unseal)
  - Generate Root
  - Leases
  - Policy
  - Auth mounts
  - Mounts
  - Keys (rotate and key status)
  - Rekey
  - Plugin Catalog

[Latest scaladoc documentation](https://ist-dsi.github.io/scala-vault/latest/api/pt/tecnico/dsi/scala-vault/index.html)

## Install
Add the following dependency to your `build.sbt`:
```sbt
libraryDependencies += "pt.tecnico.dsi" %% "scala-vault" % "0.0.0"
```
We use [semantic versioning](http://semver.org).

## License
scala-vault is open source and available under the [MIT license](LICENSE).
