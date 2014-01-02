package models

case class ChangePasswordRequest(email: String, oldPassword: String, newPassword: String)