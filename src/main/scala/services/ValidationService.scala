package com.fortysevendeg.tagless.sample
package services

import cats.data.ValidatedNel
import cats.data.Validated._
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.apply._
import com.fortysevendeg.tagless.sample.models._
import org.slf4j.{Logger, LoggerFactory}

trait ValidationService[F[_]] {

  def validateUser(
      name: String,
      age: Int,
      gender: Char,
      heightAndWeight: String): F[ValidatedNel[ValidationError, UserInformation]]

}
object ValidationService {
  def apply[F[_]](logger: Logger)(implicit S: Sync[F]): ValidationService[F] =
    new ValidationService[F] {

      def validateUser(
          name: String,
          age: Int,
          gender: Char,
          heightAndWeight: String): F[ValidatedNel[ValidationError, UserInformation]] = {
        def ageValidatedLogged: F[ValidatedNel[NotAdultError.type, Int]] =
          if (age >= 18) S.delay(logger.debug("Age is valid")).as(validNel(age))
          else S.delay(logger.debug("Age is invalid")).as(invalidNel(NotAdultError))

        def genderValidatedLogged: F[ValidatedNel[InvalidGenderError.type, Gender]] = gender match {
          case 'M' => S.delay(logger.debug("Gender is valid")).as(validNel(Male))
          case 'F' => S.delay(logger.debug("Gender is valid")).as(validNel(Female))
          case _   => S.delay(logger.debug("Gender is invalid")).as(invalidNel(InvalidGenderError))
        }

        def heightAndWeightValidatedLogged: F[
          ValidatedNel[InvalidHeightAndWeight.type, (Int, Int)]] = {
          val regex = raw"(\d+)x(\d+)".r

          heightAndWeight match {
            case regex(height, weight) =>
              S.delay(logger.debug("Height and Weight is valid"))
                .as(validNel((height.toInt, weight.toInt)))
            case _ =>
              S.delay(logger.debug("Height and Weight is valid"))
                .as(invalidNel(InvalidHeightAndWeight))
          }
        }

        for {
          ageValidated             <- ageValidatedLogged
          genderValidated          <- genderValidatedLogged
          heightAndWeightValidated <- heightAndWeightValidatedLogged
        } yield
          (ageValidated, genderValidated, heightAndWeightValidated).mapN(
            (ageV, genderV, heightAndWeightV) => UserInformation(name, ageV, genderV, heightAndWeightV._1, heightAndWeightV._2))
      }
    }

  def build[F[_]](implicit S: Sync[F]): F[ValidationService[F]] =
    S.delay(LoggerFactory.getLogger("EncryptionService")).map(ValidationService(_))
}
