// No supertype at all
class A1 {
    fun test() {
        <!SUPER_IS_NOT_AN_EXPRESSION!>super<!>.equals(null) // Call to an extension function
    }
}