package com.fauzan.kafe.ui.comments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fauzan.kafe.Adapter.MyCommentAdapter
import com.fauzan.kafe.Callback.CommentCallbackListener
import com.fauzan.kafe.Common.Common
import com.fauzan.kafe.Model.CommentModel
import com.fauzan.kafe.databinding.BottomSheetCommentFragmentBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dmax.dialog.SpotsDialog

/**
 * BottomSheet displaying customer food reviews and comments.
 * Migrated to Android Jetpack ViewBinding with nullified bindings in onDestroyView.
 */
class CommentFragment : BottomSheetDialogFragment(), CommentCallbackListener {

    private lateinit var commentViewModel: CommentViewModel
    private var _binding: BottomSheetCommentFragmentBinding? = null
    private val binding get() = _binding!!
    private var dialog: AlertDialog? = null
    private val listener: CommentCallbackListener = this

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCommentFragmentBinding.inflate(inflater, container, false)

        initView()
        loadCommentsFromFirebase()

        commentViewModel.mutableLiveDataFoodList.observe(viewLifecycleOwner) { commentModels ->
            context?.let { ctx ->
                _binding?.let { b ->
                    if (commentModels != null) {
                        val adapter = MyCommentAdapter(ctx, commentModels)
                        b.recyclerComment.adapter = adapter
                    }
                }
            }
        }

        return binding.root
    }

    private fun loadCommentsFromFirebase() {
        val food = Common.selectedFood ?: return
        dialog?.show()

        val commentModels = ArrayList<CommentModel>()
        val foodId = food.id ?: return

        FirebaseDatabase.getInstance().getReference(Common.COMMENT_REF)
            .child(foodId)
            .orderByChild("commentTimeStamp")
            .limitToLast(100)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (commentSnapShot in dataSnapshot.children) {
                        val commentModel = commentSnapShot.getValue(CommentModel::class.java)
                        if (commentModel != null) {
                            commentModels.add(commentModel)
                        }
                    }
                    listener.onCommentLoadSuccess(commentModels)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    listener.onCommentLoadFailed(databaseError.message)
                }
            })
    }

    private fun initView() {
        commentViewModel = ViewModelProvider(this).get(CommentViewModel::class.java)
        context?.let { ctx ->
            dialog = SpotsDialog.Builder().setContext(ctx).setCancelable(true).build()

            binding.recyclerComment.setHasFixedSize(true)
            val layoutManager = LinearLayoutManager(ctx, RecyclerView.VERTICAL, true)
            binding.recyclerComment.setLayoutManager(layoutManager)
            binding.recyclerComment.addItemDecoration(DividerItemDecoration(ctx, layoutManager.orientation))
        }
    }

    override fun onCommentLoadSuccess(commentModels: List<CommentModel>) {
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
        }
        commentViewModel.setCommentList(commentModels)
    }

    override fun onCommentLoadFailed(message: String) {
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
        }
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
        }
        dialog = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private var instance: CommentFragment? = null

        @JvmStatic
        fun getInstance(): CommentFragment {
            return instance ?: CommentFragment().also { instance = it }
        }
    }
}
