package net.dev.astor.util;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public final class RandomUtil {

    private RandomUtil() {}

    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;
    private static final double DOUBLE_UNIT = 0x1.0p-53;
    private static final float  FLOAT_UNIT  = 0x1.0p-24f;

    private static final AtomicLong SEED_SEQUENCE = new AtomicLong();
    private static final ThreadLocal<HybridGenerator> LOCAL =
            ThreadLocal.withInitial(new java.util.function.Supplier<HybridGenerator>() {
                @Override
                public HybridGenerator get() {
                    return RandomUtil.createHybrid();
                }
            });

    public static long nextLong(long min, long max) {
        if (min > max) throw new IllegalArgumentException("min > max");
        if (min == Long.MIN_VALUE && max == Long.MAX_VALUE) return nextRaw64();

        final long range = max - min + 1L;
        if (range > 0L) {
            if ((range & (range - 1L)) == 0L) return min + (nextRaw64() & (range - 1L));
            long u, r;
            do { u = nextRaw64() >>> 1; r = u % range; } while (u - r + (range - 1L) < 0L);
            return min + r;
        }
        long v;
        do { v = nextRaw64(); } while (Long.compareUnsigned(v, range) >= 0L);
        return min + v;
    }

    public static int nextInt(int min, int max) {
        if (min > max) throw new IllegalArgumentException("min > max");
        return (int) nextLong(min, (long) max);
    }

    public static double nextDouble(double min, double max) {
        checkRange(min, max);
        return (nextRaw64() >>> 11) * DOUBLE_UNIT * (max - min) + min;
    }

    public static float nextFloat(float min, float max) {
        checkRange(min, max);
        return (nextRaw64() >>> 40) * FLOAT_UNIT * (max - min) + min;
    }

    public static double nextDouble() { return (nextRaw64() >>> 11) * DOUBLE_UNIT; }

    public static float nextFloat() { return (nextRaw64() >>> 40) * FLOAT_UNIT; }

    public static boolean nextBoolean() { return (nextRaw64() & 1L) != 0L; }

    public static long nextRaw64() { return LOCAL.get().next64(); }

    public static double randomGaussian(double sigma) {
        double u1 = nextDouble();
        
        if (u1 <= 0.0) {
            u1 = Double.MIN_NORMAL;
        }
        double u2 = nextDouble();
        return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2) * sigma;
    }

    public static double randomGaussianInRange(double min, double max, boolean round) {
        if (min > max) {
            throw new IllegalArgumentException("min > max");
        }
        double mean = (max + min) / 2.0;
        double sigma = (max - min) / 4.0;
        double value = mean;
        int attempts = 0;
        do {
            value = randomGaussian(sigma) + mean;
            attempts++;
        } while ((value < min || value > max) && attempts < 10);
        if (attempts >= 10) {
            return mean;
        }
        return round ? Math.rint(value) : value;
    }

    private static HybridGenerator createHybrid() {
        long e = System.nanoTime();
        e ^= Long.rotateLeft(System.currentTimeMillis(), 21);
        e ^= Thread.currentThread().getId() * 0x9E3779B97F4A7C15L;
        e ^= ((long) System.identityHashCode(new Object())) << 32;
        e ^= System.identityHashCode(Thread.currentThread()) * 0xBF58476D1CE4E5B9L;
        e ^= Runtime.getRuntime().freeMemory() * 0x94D049BB133111EBL;
        e ^= Runtime.getRuntime().maxMemory() * 0xDB4F0B9175AE2165L;
        e ^= Runtime.getRuntime().totalMemory() * 0xBBE0563303A4615FL;
        e ^= SEED_SEQUENCE.incrementAndGet() * 0xC2B2AE3D27D4EB4FL;
        e ^= System.getProperty("java.version").hashCode() * 0xA24BAED4963EE407L;
        e ^= System.getProperty("os.name", "").hashCode() * 0x9FB21C651E98DF25L;
        e ^= System.getProperty("user.dir", "").hashCode() * 0xC13FA9A902A6328FL;
        e ^= osEntropy(64);
        return new HybridGenerator(e);
    }

    private static final class HybridGenerator {

        private final MT19937_64 mt;
        private final ChaCha20   chacha;
        private final CtrDrbg    drbg;
        private final Philox4x32 philox;
        private final Threefry2x64 threefry;
        private final Pcg64      pcg;
        private final long k0, k1;

        HybridGenerator(long seed) {
            long s = seed;
            this.mt       = new MT19937_64(s ^ 0xA5A5A5A5A5A5A5A5L);
            this.chacha   = new ChaCha20(s ^ 0x5A5A5A5A5A5A5A5AL);
            this.drbg     = new CtrDrbg(s ^ 0xC3C3C3C3C3C3C3C3L);
            this.philox   = new Philox4x32(s ^ 0x3C3C3C3C3C3C3C3CL);
            this.threefry = new Threefry2x64(s ^ 0xF0F0F0F0F0F0F0F0L);
            this.pcg      = new Pcg64(s ^ 0x0F0F0F0F0F0F0F0FL);
            long h = mix64(s);
            this.k0 = h;
            this.k1 = mix64(h ^ GOLDEN_GAMMA);
        }

        long next64() {
            long a = mt.next64();
            long b = chacha.next64();
            long c = drbg.next64();
            long d = philox.next64();
            long e = threefry.next64();
            long f = pcg.next64();
            long x = a
                   ^ Long.rotateLeft(b, 11)
                   ^ Long.rotateLeft(c, 22)
                   ^ Long.rotateLeft(d, 33)
                   ^ Long.rotateLeft(e, 44)
                   ^ Long.rotateLeft(f, 55);
            return sipHash48(k0, k1, x);
        }
    }

    private static final class MT19937_64 {
        private static final int NN = 312;
        private static final int MM = 156;
        private static final long MATRIX_A = 0xB5026F5AA96619E9L;
        private static final long UM = 0xFFFFFFFF80000000L;
        private static final long LM = 0x7FFFFFFFL;

        private final long[] mt = new long[NN];
        private int mti = NN + 1;

        MT19937_64(long seed) {
            mt[0] = seed;
            for (mti = 1; mti < NN; mti++) {
                mt[mti] = 6364136223846793005L * (mt[mti - 1] ^ (mt[mti - 1] >>> 62)) + mti;
            }
        }

        long next64() {
            if (mti >= NN) {
                int i;
                for (i = 0; i < NN - MM; i++) {
                    long x = (mt[i] & UM) | (mt[i + 1] & LM);
                    mt[i] = mt[i + MM] ^ (x >>> 1) ^ ((x & 1L) == 1L ? MATRIX_A : 0L);
                }
                for (; i < NN - 1; i++) {
                    long x = (mt[i] & UM) | (mt[i + 1] & LM);
                    mt[i] = mt[i + (MM - NN)] ^ (x >>> 1) ^ ((x & 1L) == 1L ? MATRIX_A : 0L);
                }
                long x = (mt[NN - 1] & UM) | (mt[0] & LM);
                mt[NN - 1] = mt[MM - 1] ^ (x >>> 1) ^ ((x & 1L) == 1L ? MATRIX_A : 0L);
                mti = 0;
            }
            long y = mt[mti++];
            y ^= (y >>> 29) & 0x5555555555555555L;
            y ^= (y << 17) & 0x71D67FFFEDA60000L;
            y ^= (y << 37) & 0xFFF7EEE000000000L;
            y ^= (y >>> 43);
            return y;
        }
    }

    private static final class ChaCha20 {
        private static final int ROUNDS = 20;

        private final int[] state = new int[16];
        private final byte[] block = new byte[64];
        private int blockIndex = 64;

        ChaCha20(long seed) {
            byte[] seedBytes = ByteBuffer.allocate(8).putLong(seed).array();
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(seedBytes);
                for (int i = 0; i < 8; i++) state[i] = readIntLE(hash, i * 4);
                byte[] h2 = md.digest(concat(seedBytes, (byte) 1));
                state[12] = readIntLE(h2, 0);
                state[13] = readIntLE(h2, 4);
                state[14] = readIntLE(h2, 8);
                state[15] = readIntLE(h2, 12);
            } catch (Exception ex) {
                throw new AssertionError(ex);
            }
        }

        long next64() {
            if (blockIndex >= 64) {
                generateBlock();
                blockIndex = 0;
            }
            long v = 0L;
            for (int i = 0; i < 8; i++) v |= (block[blockIndex + i] & 0xFFL) << (8 * i);
            blockIndex += 8;
            return v;
        }

        private void generateBlock() {
            int[] x = new int[16];
            System.arraycopy(state, 0, x, 0, 16);
            for (int i = 0; i < ROUNDS; i += 2) {
                quarterRound(x, 0, 4,  8, 12);
                quarterRound(x, 1, 5,  9, 13);
                quarterRound(x, 2, 6, 10, 14);
                quarterRound(x, 3, 7, 11, 15);
                quarterRound(x, 0, 5, 10, 15);
                quarterRound(x, 1, 6, 11, 12);
                quarterRound(x, 2, 7,  8, 13);
                quarterRound(x, 3, 4,  9, 14);
            }
            for (int i = 0; i < 16; i++) {
                int v = x[i] + state[i];
                block[i * 4    ] = (byte)  v;
                block[i * 4 + 1] = (byte) (v >>> 8);
                block[i * 4 + 2] = (byte) (v >>> 16);
                block[i * 4 + 3] = (byte) (v >>> 24);
            }
            if (++state[12] == 0) state[13]++;
        }

        private static void quarterRound(int[] x, int a, int b, int c, int d) {
            x[a] += x[b]; x[d] = Integer.rotateLeft(x[d] ^ x[a], 16);
            x[c] += x[d]; x[b] = Integer.rotateLeft(x[b] ^ x[c], 12);
            x[a] += x[b]; x[d] = Integer.rotateLeft(x[d] ^ x[a], 8);
            x[c] += x[d]; x[b] = Integer.rotateLeft(x[b] ^ x[c], 7);
        }
    }

    private static final class CtrDrbg {
        private final Aes256 aes;
        private final byte[] key = new byte[32];
        private final byte[] v   = new byte[16];
        private long outIndex = 0;

        CtrDrbg(long seed) {
            this.aes = new Aes256();
            byte[] sb = ByteBuffer.allocate(8).putLong(seed).array();
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] material = md.digest(sb);
                System.arraycopy(material, 0, key, 0, 32);
                byte[] h2 = md.digest(concat(material, (byte) 0x5A));
                System.arraycopy(h2, 0, v, 0, 16);
            } catch (Exception ex) { throw new AssertionError(ex); }
        }

        long next64() {
            if (outIndex >= 16) {
                incrementV();
                aes.encryptBlock(key, v, 0, v, 0);
                outIndex = 0;
            }
            long r = 0L;
            for (int i = 0; i < 8; i++) r |= (v[(int) outIndex + i] & 0xFFL) << (8 * i);
            outIndex += 8;
            return r;
        }

        private void incrementV() {
            for (int i = 15; i >= 0; i--) {
                if (++v[i] != 0) break;
            }
        }
    }

    private static final class Aes256 {

        private static final int[] SBOX = new int[256];
        private static final int[] RCON = {
            0x01000000, 0x02000000, 0x04000000, 0x08000000,
            0x10000000, 0x20000000, 0x40000000, 0x80000000,
            0x1B000000, 0x36000000
        };

        static {
            int p = 1, q = 1;
            do {
                p = p ^ (p << 1) ^ ((p & 0x80) != 0 ? 0x11B : 0);
                q ^= q << 1; q ^= q << 2; q ^= q << 4;
                if ((q & 0x80) != 0) q ^= 0x09;
                int x = q ^ (q << 1) ^ (q << 2) ^ (q << 3) ^ (q << 4);
                SBOX[p] = (x ^ 0x63) & 0xFF;
            } while (p != 1);
            SBOX[0] = 0x63;
        }

        void encryptBlock(byte[] key, byte[] in, int inOff, byte[] out, int outOff) {
            int[] w = expandKey(key);
            int[] s = new int[4];
            for (int i = 0; i < 4; i++) {
                s[i] = ((in[inOff + i * 4] & 0xFF) << 24)
                     | ((in[inOff + i * 4 + 1] & 0xFF) << 16)
                     | ((in[inOff + i * 4 + 2] & 0xFF) << 8)
                     | (in[inOff + i * 4 + 3] & 0xFF);
                s[i] ^= w[i];
            }
            for (int r = 1; r < 14; r++) {
                int[] t = new int[4];
                for (int i = 0; i < 4; i++) {
                    t[i] = subWord(s[i]) ^ Integer.rotateLeft(subWord(s[(i + 1) & 3]), 8)
                         ^ Integer.rotateLeft(subWord(s[(i + 2) & 3]), 16)
                         ^ Integer.rotateLeft(subWord(s[(i + 3) & 3]), 24)
                         ^ w[r * 4 + i];
                }
                s = t;
            }
            for (int i = 0; i < 4; i++) {
                int v = subWord(s[i]) ^ Integer.rotateLeft(subWord(s[(i + 1) & 3]), 8)
                      ^ Integer.rotateLeft(subWord(s[(i + 2) & 3]), 16)
                      ^ Integer.rotateLeft(subWord(s[(i + 3) & 3]), 24)
                      ^ w[56 + i];
                out[outOff + i * 4    ] = (byte) (v >>> 24);
                out[outOff + i * 4 + 1] = (byte) (v >>> 16);
                out[outOff + i * 4 + 2] = (byte) (v >>> 8);
                out[outOff + i * 4 + 3] = (byte)  v;
            }
        }

        private static int subWord(int x) {
            return (SBOX[(x >>> 24) & 0xFF] << 24)
                 | (SBOX[(x >>> 16) & 0xFF] << 16)
                 | (SBOX[(x >>> 8) & 0xFF] << 8)
                 | SBOX[x & 0xFF];
        }

        private static int[] expandKey(byte[] key) {
            int[] w = new int[60];
            for (int i = 0; i < 8; i++) {
                w[i] = ((key[i * 4] & 0xFF) << 24)
                     | ((key[i * 4 + 1] & 0xFF) << 16)
                     | ((key[i * 4 + 2] & 0xFF) << 8)
                     | (key[i * 4 + 3] & 0xFF);
            }
            for (int i = 8; i < 60; i++) {
                int t = w[i - 1];
                if (i % 8 == 0) t = subWord(Integer.rotateLeft(t, 8)) ^ RCON[i / 8 - 1];
                else if (i % 8 == 4) t = subWord(t);
                w[i] = w[i - 8] ^ t;
            }
            return w;
        }
    }

    private static final class Philox4x32 {

        private static final int M0 = 0xD2511F53;
        private static final int M1 = 0xCD9E8D57;
        private static final int W0 = 0x9E3779B9;
        private static final int W1 = 0xBB67AE85;
        private static final int ROUNDS = 10;

        private int c0, c1, c2, c3;
        private int k0, k1;

        Philox4x32(long seed) {
            this.c0 = (int) seed;
            this.c1 = (int) (seed >>> 32);
            this.c2 = (int) mix64(seed);
            this.c3 = (int) (mix64(seed) >>> 32);
            this.k0 = (int) mix64(seed ^ 0x9E3779B97F4A7C15L);
            this.k1 = (int) (mix64(seed ^ 0xBF58476D1CE4E5B9L) >>> 32);
        }

        long next64() {
            int x0 = c0, x1 = c1, x2 = c2, x3 = c3;
            int kk0 = k0, kk1 = k1;
            for (int i = 0; i < ROUNDS; i++) {
                long p0 = (M0 & 0xFFFFFFFFL) * (x0 & 0xFFFFFFFFL);
                long p1 = (M1 & 0xFFFFFFFFL) * (x2 & 0xFFFFFFFFL);
                x0 = (int) ((p1 >>> 32) ^ (x1 & 0xFFFFFFFFL) ^ (kk0 & 0xFFFFFFFFL));
                x1 = (int) p1;
                x2 = (int) ((p0 >>> 32) ^ (x3 & 0xFFFFFFFFL) ^ (kk1 & 0xFFFFFFFFL));
                x3 = (int) p0;
                if (i < ROUNDS - 1) {
                    kk0 += W0;
                    kk1 += W1;
                }
            }
            c0 = x0; c1 = x1; c2 = x2; c3 = x3;
            if (++c3 == 0) {
                if (++c2 == 0) {
                    if (++c1 == 0) ++c0;
                }
            }
            long hi = ((long) x0 << 32) | (x1 & 0xFFFFFFFFL);
            long lo = ((long) x2 << 32) | (x3 & 0xFFFFFFFFL);
            return hi ^ Long.rotateLeft(lo, 17);
        }
    }

    private static final class Threefry2x64 {

        private static final long C240 = 0x1BD11BDAA9FC1A22L;
        private static final int ROUNDS = 20;
        private static final int[] ROT = {
            16, 42, 12, 31, 16, 32, 24, 21
        };

        private long x0, x1;
        private final long k0, k1, k2;

        Threefry2x64(long seed) {
            long a = mix64(seed);
            long b = mix64(seed ^ 0x9E3779B97F4A7C15L);
            this.k0 = a;
            this.k1 = b;
            this.k2 = C240 ^ a ^ b;
            this.x0 = mix64(seed ^ 0xBF58476D1CE4E5B9L);
            this.x1 = mix64(seed ^ 0x94D049BB133111EBL);
        }

        long next64() {
            long a = x0, b = x1;
            for (int i = 0; i < ROUNDS; i++) {
                a += b;
                b = Long.rotateLeft(b, ROT[i & 7]);
                b ^= a;
                int r = i + 1;
                if ((r & 3) == 0) {
                    int rk = r >> 2;
                    a += ks(rk);
                    b += ks(rk + 1) + rk;
                }
            }
            x0 = a; x1 = b;
            x0 += GOLDEN_GAMMA;
            if (x0 == 0) x1++;
            return a ^ Long.rotateLeft(b, 29);
        }

        private long ks(int idx) {
            int m = idx % 3;
            if (m == 0) return k0;
            if (m == 1) return k1;
            return k2;
        }
    }

    private static final class Pcg64 {

        private static final long MULT_HI = 0x2360ED051FC65DA4L;
        private static final long MULT_LO = 0x4385DF649FCCF645L;

        private long stateHi;
        private long stateLo;
        private final long incHi;
        private final long incLo;

        Pcg64(long seed) {
            this.stateHi = mix64(seed ^ 0xA24BAED4963EE407L);
            this.stateLo = mix64(seed ^ 0x9FB21C651E98DF25L);
            this.incHi = mix64(seed ^ 0xC13FA9A902A6328FL) | 0x8000000000000000L;
            this.incLo = mix64(seed ^ 0xDA942042E4DD58B5L) | 1L;
            step();
            step();
        }

        long next64() {
            long oldHi = stateHi;
            long oldLo = stateLo;
            step();
            long xored = oldHi ^ oldLo;
            int rot = (int) (oldHi >>> 58);
            return Long.rotateRight(xored, rot);
        }

        private void step() {
            long aLo = stateLo & 0xFFFFFFFFL;
            long aHi = stateLo >>> 32;
            long bLo = MULT_LO & 0xFFFFFFFFL;
            long bHi = MULT_LO >>> 32;

            long p0 = aLo * bLo;
            long p1 = aLo * bHi;
            long p2 = aHi * bLo;
            long p3 = aHi * bHi;

            long mid = (p0 >>> 32) + (p1 & 0xFFFFFFFFL) + (p2 & 0xFFFFFFFFL);
            long newLo = (p0 & 0xFFFFFFFFL) | (mid << 32);
            long newHi = p3 + (p1 >>> 32) + (p2 >>> 32) + (mid >>> 32);

            long cLo = stateLo * MULT_HI;
            long cHi = stateHi * MULT_HI;
            newHi += cLo + cHi;

            newLo += incLo;
            if (Long.compareUnsigned(newLo, incLo) < 0) newHi++;
            newHi += incHi;

            stateLo = newLo;
            stateHi = newHi;
        }
    }

    private static long sipHash48(long k0, long k1, long data) {
        long v0 = k0 ^ 0x736F6D6570736575L;
        long v1 = k1 ^ 0x646F72616E646F6DL;
        long v2 = k0 ^ 0x6C7967656E657261L;
        long v3 = k1 ^ 0x7465646279746573L;

        v3 ^= data;
        for (int i = 0; i < 4; i++) {
            v0 += v1; v1 = Long.rotateLeft(v1, 13); v1 ^= v0; v0 = Long.rotateLeft(v0, 32);
            v2 += v3; v3 = Long.rotateLeft(v3, 16); v3 ^= v2;
            v0 += v3; v3 = Long.rotateLeft(v3, 21); v3 ^= v0;
            v2 += v1; v1 = Long.rotateLeft(v1, 17); v1 ^= v2; v2 = Long.rotateLeft(v2, 32);
        }
        v0 ^= data;

        long b = 8L << 56;
        v3 ^= b;
        for (int i = 0; i < 4; i++) {
            v0 += v1; v1 = Long.rotateLeft(v1, 13); v1 ^= v0; v0 = Long.rotateLeft(v0, 32);
            v2 += v3; v3 = Long.rotateLeft(v3, 16); v3 ^= v2;
            v0 += v3; v3 = Long.rotateLeft(v3, 21); v3 ^= v0;
            v2 += v1; v1 = Long.rotateLeft(v1, 17); v1 ^= v2; v2 = Long.rotateLeft(v2, 32);
        }
        v0 ^= b;
        v2 ^= 0xFF;

        for (int i = 0; i < 8; i++) {
            v0 += v1; v1 = Long.rotateLeft(v1, 13); v1 ^= v0; v0 = Long.rotateLeft(v0, 32);
            v2 += v3; v3 = Long.rotateLeft(v3, 16); v3 ^= v2;
            v0 += v3; v3 = Long.rotateLeft(v3, 21); v3 ^= v0;
            v2 += v1; v1 = Long.rotateLeft(v1, 17); v1 ^= v2; v2 = Long.rotateLeft(v2, 32);
        }
        return v0 ^ v1 ^ v2 ^ v3;
    }

    private static long osEntropy(int bytes) {
        long acc = 0L;
        InputStream in = null;
        try {
            Path p = Paths.get("/dev/urandom");
            if (Files.isReadable(p)) {
                byte[] buf = new byte[bytes];
                in = Files.newInputStream(p);
                int n = 0;
                while (n < buf.length) {
                    int r = in.read(buf, n, buf.length - n);
                    if (r < 0) break;
                    n += r;
                }
                for (int i = 0; i + 8 <= n; i += 8) {
                    long v = 0L;
                    for (int j = 0; j < 8; j++) v = (v << 8) | (buf[i + j] & 0xFFL);
                    acc ^= Long.rotateLeft(v, (i * 3) & 63);
                }
                for (int i = 0; i < n; i++) {
                    acc ^= ((long) (buf[i] & 0xFF)) << ((i * 5) & 56);
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (in != null) {
                try { in.close(); } catch (Throwable ignored) {}
            }
        }
        return acc;
    }

    private static void checkRange(double min, double max) {
        if (!(min <= max)) throw new IllegalArgumentException("bad range: [" + min + ", " + max + "]");
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static int readIntLE(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8)
             | ((b[off + 2] & 0xFF) << 16) | ((b[off + 3] & 0xFF) << 24);
    }

    private static byte[] concat(byte[] a, byte b) {
        byte[] r = Arrays.copyOf(a, a.length + 1);
        r[a.length] = b;
        return r;
    }
}