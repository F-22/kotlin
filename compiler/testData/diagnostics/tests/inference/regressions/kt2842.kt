package c

trait A<T>

fun test(a: A<Int>?) {
    a<!UNSAFE_CALL!>.<!>foo() //no error
}

fun <R> A<R>.foo() {}

//------------
fun test(nullabilityInfoMap: Map<Int, Any>?) {
    nullabilityInfoMap<!UNSAFE_CALL!>.<!>iterator() //no error
}

//resolves to
public inline fun <K,V> Map<K,V>.iterator(): Iterator<Map.Entry<K,V>> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>


//-------------
fun foo() : Boolean {
    val nullableList = getNullableList()
    return nullableList<!UNSAFE_CALL!>.<!>contains("")

}

fun getNullableList(): List<String>? = null