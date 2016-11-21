//
// Created by clebeaupin on 05/02/16.
//

#include "Service.h"
#include <public/ILicense.h>
#include <public/LcpStatus.h>
#include <LcpUtils.h>
#include <public/ILcpService.h>

#include <ePub3/utilities/utfstring.h>

JNIEXPORT void JNICALL Java_org_readium_sdk_lcp_Service_nativeInjectLicense(
        JNIEnv *env, jobject obj, jlong servicePtr, jstring jEpubPath, jstring jLicenseJson) {

     lcp::ILcpService * service = (lcp::ILcpService *) servicePtr;

     const char * cEpubPath = env->GetStringUTFChars(jEpubPath, 0);
     std::string epubPath(cEpubPath);

     const char * cLicenseJson = env->GetStringUTFChars(jLicenseJson, 0);
     std::string licenseJson(cLicenseJson);

     service->InjectLicense(epubPath, licenseJson);
}
//
//JNIEXPORT void JNICALL Java_org_readium_sdk_lcp_Service_nativeInjectLicense(
//        JNIEnv *env, jobject obj, jlong servicePtr, jstring jEpubPath, jobject jLicense) {
//
//     lcp::ILcpService * service = (lcp::ILcpService *) servicePtr;
//
//     const char * cEpubPath = env->GetStringUTFChars(jEpubPath, 0);
//     std::string epubPath(cEpubPath);
//
////     jLicense = env->NewGlobalRef(jLicense);
//
//     jclass jLicenseClass = env->GetObjectClass(jLicense);
//     // TODO
//
//     service->InjectLicense(epubPath, license);
//
//     //env->DeleteGlobalRef(jLicense);
//}

JNIEXPORT jobject JNICALL Java_org_readium_sdk_lcp_Service_nativeOpenLicense(
        JNIEnv *env, jobject obj, jlong servicePtr, jstring jLicenseJson) {

     lcp::ILcpService * service = (lcp::ILcpService *) servicePtr;

     const char * cLicenseJson = env->GetStringUTFChars(jLicenseJson, 0);
     std::string licenseJson(cLicenseJson);

     lcp::ILicense* license = nullptr;
     lcp::ILicense** licensePTR = &license;

     const std::string epubPath("");

     try {
          lcp::Status status = service->OpenLicense(
                  epubPath,
                  licenseJson,
                  licensePTR);

          if (!lcp::Status::IsSuccess(status)) {
               // An error has occured
               // TODO: catch error in a handler
               return nullptr;
          }
     } catch (lcp::StatusException &ex) {
          // TODO: catch error in a handler
          return nullptr;
     }

     jclass cls = env->FindClass("org/readium/sdk/lcp/License");
     jmethodID methodId = env->GetMethodID(cls, "<init>", "(JJ)V");
     return env->NewObject(cls, methodId, (jlong) (*licensePTR), (jlong) service);
}
//
//#if !DISABLE_LSD
//
//JNIEXPORT void JNICALL Java_org_readium_sdk_lcp_Service_nativeNotifyLicenseStatusDocumentProcessingCancelled(
//        JNIEnv *env, jobject obj, jlong servicePtr) {
//
//     lcp::ILcpService * service = (lcp::ILcpService *) servicePtr;
//     service->NotifyLicenseStatusDocumentProcessingCancelled();
//}
//
//#endif //!DISABLE_LSD
