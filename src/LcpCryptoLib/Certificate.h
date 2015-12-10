//
//  Created by Artem Brazhnikov on 11/15.
//  Copyright © 2015 Mantano. All rights reserved.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY
//  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//  Licensed under Gnu Affero General Public License Version 3 (provided, notwithstanding this notice,
//  Readium Foundation reserves the right to license this material under a different separate license,
//  and if you have done so, the terms of that separate license control and the following references
//  to GPL do not apply).
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the GNU
//  Affero General Public License as published by the Free Software Foundation, either version 3 of
//  the License, or (at your option) any later version. You should have received a copy of the GNU
//  Affero General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
//

#ifndef __CERTIFICATE_H__
#define __CERTIFICATE_H__

#include <string>
#include <cryptopp/rsa.h>
#include <cryptopp/secblock.h>
#include "ICertificate.h"

using namespace CryptoPP;

namespace lcp
{
    class IEncryptionProfile;
    class ISignatureAlgorithm;

    class Certificate : public ICertificate
    {
    public:
        explicit Certificate(
            const std::string & certificateBase64,
            IEncryptionProfile * encryptionProfile
            );

        std::string SerialNumber() const;
        std::string NotBeforeDate() const;
        std::string NotAfterDate() const;
        KeyType PublicKey() const;

        bool VerifyCertificate(ICertificate * rootCertificate);
        bool VerifyMessage(const std::string & message, const std::string & hashBase64);
        bool VerifyMessage(
            const unsigned char * message,
            size_t messageLength,
            const unsigned char * signature,
            size_t signatureLength
            );

    private:
        std::string m_serialNumber;
        std::string m_notBeforeDate;
        std::string m_notAfterDate;
        RSA::PublicKey m_publicKey;
        SecByteBlock m_toBeSignedData;
        SecByteBlock m_rootSignature;
        OID m_signatureAlgorithmId;

        IEncryptionProfile * m_encryptionProfile;
        std::unique_ptr<ISignatureAlgorithm> m_signatureAlgorithm;

        static const int Certificatev3 = 2;
    };
}

#endif //__CERTIFICATE_H__
