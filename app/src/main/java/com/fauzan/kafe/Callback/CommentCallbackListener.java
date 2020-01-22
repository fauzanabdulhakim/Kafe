package com.fauzan.kafe.Callback;

import com.fauzan.kafe.Model.CommentModel;

import java.util.List;

public interface CommentCallbackListener {
    void onCommentLoadSuccess(List<CommentModel> commentModels);
    void onCommentLoadFailed(String message);
}
