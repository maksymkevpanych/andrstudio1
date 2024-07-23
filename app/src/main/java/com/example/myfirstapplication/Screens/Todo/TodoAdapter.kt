package com.example.myfirstapplication

import android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class TodoAdapter(private var todos: MutableList<Todo>) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        return TodoViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_todo,
                parent,
                false
            )
        )
    }

    fun addTodo(todo: Todo) {
        // Modify the title to always start with capital letter and end with a dot
        val formattedTitle = todo.title.trim().
        replaceFirstChar { if (it. isLowerCase()) it. titlecase(Locale. getDefault()) else it. toString() } + "."
        todos.add(todo.copy(title = formattedTitle))
        notifyItemInserted(todos.size - 1)
    }

    fun deleteDoneTodos() {
        val iterator = todos.iterator()
        while (iterator.hasNext()) {
            val todo = iterator.next()
            if (todo.isChecked) {
                iterator.remove()
            }
        }
        notifyDataSetChanged()

        //updateList(todos)

    }

    private fun toggleStrikeThrough(tvTodoTitle: TextView, isChecked: Boolean) {
        if (isChecked) {
            tvTodoTitle.paintFlags = tvTodoTitle.paintFlags or STRIKE_THRU_TEXT_FLAG
        } else {
            tvTodoTitle.paintFlags = tvTodoTitle.paintFlags and STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val curTodo = todos[position]
        holder.itemView.apply {
            val tvTodoTitle: TextView = findViewById(R.id.tvTodoTitle)
            val cbDone: CheckBox = findViewById(R.id.cbDone)
            tvTodoTitle.text = curTodo.title
            cbDone.isChecked = curTodo.isChecked
            toggleStrikeThrough(tvTodoTitle, curTodo.isChecked)
            cbDone.setOnCheckedChangeListener { _, isChecked ->
                toggleStrikeThrough(tvTodoTitle, isChecked)
                curTodo.isChecked = isChecked
            }
        }
    }

    override fun getItemCount(): Int {
        return todos.size
    }

    fun updateList(newList: List<Todo>) {
        val diffResult = DiffUtil.calculateDiff(TodoDiffCallback(newList, todos))
        todos.clear()
        todos.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    private class TodoDiffCallback(
        private val newTodos: List<Todo>,
        private val oldTodos: List<Todo>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldTodos.size
        }

        override fun getNewListSize(): Int {
            return newTodos.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldTodos[oldItemPosition].id == newTodos[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldTodos[oldItemPosition] == newTodos[newItemPosition]
        }
    }
}