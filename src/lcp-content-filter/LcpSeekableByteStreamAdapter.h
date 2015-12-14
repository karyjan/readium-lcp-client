//
//  Created by Mickaël Menu on 02/12/15.
//  Copyright © 2015 Mantano. All rights reserved.
//  Any commercial use is strictly prohibited.
//

#ifndef __LCP_BYTE_STREAM_ADAPTER_H__
#define __LCP_BYTE_STREAM_ADAPTER_H__

#include "StreamInterfaces.h"
#include <ePub3/utilities/byte_stream.h>

namespace lcp
{
    class SeekableByteStreamAdapter : public IReadableStream
    {
    public:
        SeekableByteStreamAdapter(ePub3::SeekableByteStream *byteStream) : m_byteStream(byteStream) {}
        
        virtual void Read(unsigned char * pBuffer, int64_t sizeToRead)
        {
            m_byteStream->ReadBytes(pBuffer, sizeToRead);
        }
        
        virtual void SetReadPosition(int64_t pos)
        {
            m_byteStream->Seek(pos, std::ios::beg);
        }
        
        virtual int64_t ReadPosition() const
        {
            return m_byteStream->Position();
        }
        
        virtual int64_t Size()
        {
            return m_byteStream->Position() + m_byteStream->BytesAvailable();
        }
        
    protected:
        ePub3::SeekableByteStream *m_byteStream;
    };
}

#endif // __LCP_BYTE_STREAM_ADAPTER_H__