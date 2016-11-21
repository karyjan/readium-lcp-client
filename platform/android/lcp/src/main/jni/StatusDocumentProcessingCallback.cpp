//
//#if !DISABLE_LSD
//
//#include "StatusDocumentProcessingCallback.h"
//#include "Util.h"
//
//namespace lcp {
//    StatusDocumentProcessingCallback::StatusDocumentProcessingCallback(jobject jStatusDocumentProcessingCallback) {
//        JNIEnv * env = getJNIEnv();
//        this->jStatusDocumentProcessingCallback = env->NewGlobalRef(jStatusDocumentProcessingCallback);
//        jclass jStatusDocumentProcessingCallbackClass = env->GetObjectClass(this->jStatusDocumentProcessingCallback);
//        this->jOnStatusDocumentProcessingCompleteMethodId = env->GetMethodID(
//                jStatusDocumentProcessingCallbackClass, "onStatusDocumentProcessingComplete", "()V");
//    }
//
//    StatusDocumentProcessingCallback::~StatusDocumentProcessingCallback() {
//        JNIEnv * env = getJNIEnv();
//        env->DeleteGlobalRef(this->jStatusDocumentProcessingCallback);
//    }
//
//    void StatusDocumentProcessingCallback::OnStatusDocumentProcessingComplete(IStatusDocumentProcessing* sdp) {
//        JNIEnv * env = getJNIEnv();
//        env->CallVoidMethod(
//                this->jStatusDocumentProcessingCallback, jOnStatusDocumentProcessingCompleteMethodId);
//    }
//}
//
//#endif //!DISABLE_LSD