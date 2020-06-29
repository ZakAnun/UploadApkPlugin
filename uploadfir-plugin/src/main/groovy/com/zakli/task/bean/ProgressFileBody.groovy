package com.zakli.task.bean

import org.apache.commons.io.output.CountingOutputStream
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.content.FileBody

import java.math.RoundingMode

class ProgressFileBody extends FileBody {

    private IStreamListener listener
    private long writtenLength
    private Integer tempResult

    ProgressFileBody(File file) {
        this(file, ContentType.DEFAULT_BINARY)
    }

    ProgressFileBody(File file, ContentType contentType) {
        this(file, contentType, "DefaultProgressFile")
    }

    ProgressFileBody(File file, ContentType contentType, String filename) {
        super(file, contentType, filename)
    }

    @Override
    void writeTo(OutputStream out) throws IOException {
        BigDecimal contentLength = new BigDecimal(getContentLength())
        BigDecimal hundred = new BigDecimal(100)
        CountingOutputStream outputStream = new CountingOutputStream(out) {
            @Override
            protected synchronized void beforeWrite(int n) {
                if (listener != null &&
                        n != 0) {
                    writtenLength += n
                    BigDecimal result = new BigDecimal(writtenLength)
                            .divide(contentLength, 2, RoundingMode.HALF_UP) * hundred
                    if (tempResult != result.toInteger()) {
                        tempResult = result.toInteger()
                        listener.onProgress(result.toInteger())
                    }
                }
                super.beforeWrite(n)
            }
        }

        super.writeTo(outputStream)
    }

    public void setListener(IStreamListener listener) {
        this.listener = listener
    }

    public IStreamListener getListener() {
        return listener
    }
}