package com.example.myfirstapplication

data class UserModel(
    val id: Int,
    val name: String,
    val username: String,
    val email: String,
    val address: Address,
    val company: Company
)

data class Address(
    val street: String,
    val suite: String,
    val city: String,
    val zipcode: String
)

data class Company(
    val name: String
)
