package com.gitub.oopgurus.refactoringproblems.configserver

interface PersonVisitor {

    fun id(id: Long)

    fun name(name: String)

    fun firstName(firstName: String)

    fun lastName(lastName: String)

    fun email(email: String)

    fun phone(phone: String)

    fun isKorean(isKorean: Boolean)

    fun isMobilePhone(isMobilePhone: Boolean)

    fun isOfficePhone(isOfficePhone: Boolean)
}