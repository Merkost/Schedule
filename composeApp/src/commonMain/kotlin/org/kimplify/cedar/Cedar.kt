package org.kimplify.cedar

object Cedar {
    private val trees = mutableListOf<Tree>()

    fun plant(tree: Tree) {
        trees += tree
    }

    fun tag(tag: String): TaggedLogger = TaggedLogger(tag)

    internal fun emit(level: Level, tag: String, message: String, throwable: Throwable?) {
        if (trees.isEmpty()) {
            ConsoleTree.log(level, tag, message, throwable)
            return
        }
        trees.forEach { it.log(level, tag, message, throwable) }
    }
}

enum class Level { V, D, I, W, E }

interface Tree {
    fun log(level: Level, tag: String, message: String, throwable: Throwable?)
}

object ConsoleTree : Tree {
    override fun log(level: Level, tag: String, message: String, throwable: Throwable?) {
        println("${level.name}/$tag: $message${throwable?.let { " — ${it::class.simpleName}: ${it.message}" } ?: ""}")
        throwable?.printStackTrace()
    }
}

class TaggedLogger(private val tag: String) {
    fun v(message: String, throwable: Throwable? = null) = Cedar.emit(Level.V, tag, message, throwable)
    fun d(message: String, throwable: Throwable? = null) = Cedar.emit(Level.D, tag, message, throwable)
    fun i(message: String, throwable: Throwable? = null) = Cedar.emit(Level.I, tag, message, throwable)
    fun w(message: String, throwable: Throwable? = null) = Cedar.emit(Level.W, tag, message, throwable)
    fun e(message: String, throwable: Throwable? = null) = Cedar.emit(Level.E, tag, message, throwable)
}
