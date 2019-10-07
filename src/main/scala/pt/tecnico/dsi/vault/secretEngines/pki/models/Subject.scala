package pt.tecnico.dsi.vault.secretEngines.pki.models

import io.circe.derivation._

object Subject {
  import pt.tecnico.dsi.vault.{encodeArrayAsCSV, decodeArrayAsCSV}

  implicit val encoder = deriveEncoder[Subject](renaming.snakeCase, None)
  implicit val decoder = deriveDecoder[Subject](renaming.snakeCase, false, None)

  // These default values are sneaky. They only work because encodeArrayAsCSV(Array.empty) === encodeArrayAsCSV(Array(""))
  /*def apply(ou: String = "", organization: String = "", country: String = "", locality: String = "",
            province: String = "", streetAddress: String = "", postalCode: String = ""): Subject =
    new Subject(Array(ou), Array(organization), Array(country), Array(locality), Array(province), Array(streetAddress), Array(postalCode))*/
}

/**
  * @param ou the OU (OrganizationalUnit) values in the subject field.
  * @param organization the O (Organization) values in the subject field.
  * @param country the C (Country) values in the subject field.
  * @param locality the L (Locality) values in the subject field.
  * @param province the ST (Province) values in the subject field.
  * @param streetAddress the Street Address values in the subject field.
  * @param postalCode the Postal Code values in the subject field.
  */
case class Subject(ou: Array[String] = Array.empty, organization: Array[String] = Array.empty,
                   country: Array[String] = Array.empty, locality: Array[String] = Array.empty, province: Array[String] = Array.empty,
                   streetAddress: Array[String] = Array.empty, postalCode: Array[String] = Array.empty)
