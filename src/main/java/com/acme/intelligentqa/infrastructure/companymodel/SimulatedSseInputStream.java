package com.acme.intelligentqa.infrastructure.companymodel;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 * 模拟 SSE 数据流输入流，按帧延迟输出原始网络字节，模拟外部流式大模型生成时延。
 */
public class SimulatedSseInputStream extends InputStream {

    /**
     * 待输出的数据帧字节列表。
     */
    private final List<byte[]> frames;

    /**
     * 当前正在读取的数据帧索引。
     */
    private int frameIndex = 0;

    /**
     * 当前帧内部已读取的字节偏移量。
     */
    private int byteIndex = 0;

    /**
     * 每帧之间等待的毫秒数。
     */
    private final long delayMillis;

    /**
     * 创建默认 70 毫秒帧延迟的模拟流式输入流。
     *
     * @param frames SSE 数据帧文本列表。
     */
    public SimulatedSseInputStream(final List<String> frames) {
        this(frames, 70L);
    }

    /**
     * 创建指定帧延迟的模拟流式输入流。
     *
     * @param frames SSE 数据帧文本列表。
     * @param delayMillis 每帧之间的延迟毫秒数。
     */
    public SimulatedSseInputStream(final List<String> frames, final long delayMillis) {
        this.delayMillis = Math.max(0L, delayMillis);
        this.frames = new ArrayList<>(frames.size());
        for (final String frame : frames) {
            this.frames.add(frame.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 读取下一个字节；跨帧时按配置延迟挂起当前线程以模拟网络流速。
     *
     * @return 字节值（0-255），到达流末尾时返回 -1。
     */
    @Override
    public int read() {
        if (frameIndex >= frames.size()) {
            return -1;
        }
        final byte[] currentFrame = frames.get(frameIndex);
        if (byteIndex < currentFrame.length) {
            return currentFrame[byteIndex++] & 0xFF;
        }
        frameIndex++;
        byteIndex = 0;
        if (delayMillis > 0L && frameIndex < frames.size()) {
            LockSupport.parkNanos(delayMillis * 1_000_000L);
        }
        return read();
    }

    /**
     * 批量读取字节数组，保持流式帧边界独立性。
     *
     * @param b 存储读取数据的字节缓冲区。
     * @param off 缓冲区起始偏移量。
     * @param len 最大读取字节数。
     * @return 实际读取字节数，到达流末尾时返回 -1。
     */
    @Override
    public int read(final byte[] b, final int off, final int len) {
        validateReadArguments(b, off, len);
        if (len == 0) {
            return 0;
        }
        final int firstByte = read();
        if (firstByte == -1) {
            return -1;
        }
        b[off] = (byte) firstByte;
        return fillRemainingBytes(b, off, len);
    }

    /**
     * 校验批量读取入参合法性。
     *
     * @param buffer 目标字节缓冲区。
     * @param offset 缓冲区偏移量。
     * @param length 读取字节长度。
     */
    private static void validateReadArguments(final byte[] buffer, final int offset, final int length) {
        if (buffer == null) {
            throw new NullPointerException("buffer must not be null");
        }
        if (offset < 0 || length < 0 || length > buffer.length - offset) {
            throw new IndexOutOfBoundsException("offset or length out of range");
        }
    }

    /**
     * 在当前帧剩余可用范围内填充后续字节。
     *
     * @param buffer 目标字节缓冲区。
     * @param offset 缓冲区偏移量。
     * @param length 最大读取长度。
     * @return 实际累计填充的字节数。
     */
    private int fillRemainingBytes(final byte[] buffer, final int offset, final int length) {
        int bytesRead = 1;
        while (bytesRead < length && byteIndex > 0 && frameIndex < frames.size()) {
            final int nextByte = read();
            if (nextByte == -1) {
                break;
            }
            buffer[offset + bytesRead] = (byte) nextByte;
            bytesRead++;
        }
        return bytesRead;
    }

    /**
     * 关闭当前输入流并清空读取状态。
     */
    @Override
    public void close() {
        frameIndex = frames.size();
    }
}
