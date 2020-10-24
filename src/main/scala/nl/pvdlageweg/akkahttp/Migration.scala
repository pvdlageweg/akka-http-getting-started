package nl.pvdlageweg.akkahttp

import org.flywaydb.core.Flyway
import com.typesafe.config.ConfigFactory
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.api.output.MigrateResult

object Migration {

  private val config = ConfigFactory.load()
  private val databaseConfig = config.getConfig("db.auctions")

  private val flywayConfiguration = new FluentConfiguration()
  flywayConfiguration.dataSource(
    databaseConfig.getString("db.url"),
    databaseConfig.getString("db.user"),
    databaseConfig.getString("db.password")
  )
  private val flyway = new Flyway(flywayConfiguration)

  def migrate(): MigrateResult = {
    flyway.migrate()
  }

  def reloadSchema(): MigrateResult = {
    flyway.clean()
    flyway.migrate()
  }

}
