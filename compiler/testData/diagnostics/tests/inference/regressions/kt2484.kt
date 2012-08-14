//KT-2484 Type inferred for function literal is (...) -> Int instead of (...) -> Unit, when function literal parameter is explicit 

package a
//+JDK
import java.util.List

fun <T> Array<T>.forEach(operation: (T) -> Unit) : Unit = for (element in this) operation(element)

fun bar(operation: (String) -> Unit) = operation("")

fun main(args: Array<String>) {
    args.forEach { (a : String) : Unit -> a.length } // Type mismatch: (String) -> Unit required, (String) -> Int found
    args.forEach { (a) : Unit -> a.length } // Type mismatch: (String) -> Unit required, (String) -> Int found
    args.forEach { (a : String) -> a.length } // Type mismatch: (String) -> Unit required, (String) -> Int found
    args.forEach { a -> a.length } // Type mismatch: (String) -> Unit required, (String) -> Int found
    args.forEach { it.length }     // This works!

    bar { (a: String) : Unit -> a.length }
    bar { (a) : Unit -> a.length }
    bar { (a: String) -> a.length }
    bar { a -> a.length }
    bar { it.length }
}