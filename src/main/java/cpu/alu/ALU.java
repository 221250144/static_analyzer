package cpu.alu;

import util.DataType;
import util.Transformer;

public class ALU {
    /**
     * 返回两个二进制整数的和
     * dest + src
     *
     * @param src  32-bits
     * @param dest 32-bits
     * @return 32-bits
     */
    public DataType add(DataType src, DataType dest) {
        String srcStr = src.toString();
        String destStr = dest.toString();
        String ans = "";
        int[] num1 = new int[32];
        int[] num2 = new int[32];
        for (int i = 0; i <= 31; i++) {
            num1[i] = srcStr.charAt(i) - '0';
            num2[i] = destStr.charAt(i) - '0';
        }
        for (int i = 31; i >= 0; i--) {
            num1[i] += num2[i];
            if (num1[i] == 2) {
                num1[i] = 0;
                if (i != 0) {
                    num1[i - 1]++;
                }
            } else if (num1[i] == 3) {
                num1[i] = 1;
                if (i != 0) {
                    num1[i - 1]++;
                }
            }
        }
        if (num1[0] == 2) {
            num1[0] = 0;
        } else if (num1[0] == 3) {
            num1[0] = 1;
        }
        for (int i = 0; i <= 31; i++) {
            ans += num1[i];
        }
        return new DataType(ans);
    }

    /**
     * 返回两个二进制整数的差
     * dest - src
     *
     * @param src  32-bits
     * @param dest 32-bits
     * @return 32-bits
     */
    public DataType sub(DataType src, DataType dest) {
        String srcStr = src.toString();
        int[] num1 = new int[32];
        for (int i = 0; i <= 31; i++) {
            num1[i] = srcStr.charAt(i) - '0';
            num1[i] = 1 - num1[i];
        }
        num1[31]++;
        for (int i = 31; i >= 0; i--) {
            if (num1[i] == 2) {
                num1[i] = 0;
                if (i != 0) {
                    num1[i - 1]++;
                }
            }
        }
        if (num1[0] == 2) {
            num1[0] = 0;
        }
        String ans = "";
        for (int i = 0; i <= 31; i++) {
            ans += num1[i];
        }
        return add(new DataType(ans), dest);
    }

    /**
     * 返回两个二进制整数的乘积(结果低位截取后32位)
     * dest * src
     *
     * @param src  32-bits
     * @param dest 32-bits
     * @return 32-bits
     */
    public DataType mul(DataType src, DataType dest) {
        String tail1 = src.toString();
        String tail2 = dest.toString();
        int offset = 0;

        int[] N1 = new int[54];
        int[] N2 = new int[55];
        int[] N3 = new int[54];
        N2[54] = 0;
        for (int i = 27; i < 54; i++) {
            N1[i] = tail1.charAt(i - 27) - '0';
            N2[i] = tail2.charAt(i - 27) - '0';
            N3[i] = 1 - N1[i];
        }

        N3[53] += 1; // 取反加一 （不清楚如何调用已有的方法）
        for (int i = 53; i > 26; i--) {
            if (N3[i] == 2) {
                N3[i] = 0;
                N3[i - 1]++;
            }
        }
        if (N3[26] == 2) {
            N3[26] = 0;
        }

        int[] ans = new int[54];
        for (int i = 54; i > 27; i--) {
            if (N2[i] == 0 && N2[i - 1] == 1) {
                for (int j = 27; j < 54; j++) {
                    ans[j - offset] += N3[j];
                }
            } else if (N2[i] == 1 && N2[i - 1] == 0) {
                for (int j = 27; j < 54; j++) {
                    ans[j - offset] += N1[j];
                }
            }
            for (int j = 53 - offset; j > 27 - offset; j--) {
                if (ans[j] == 2) {
                    ans[j] = 0;
                    ans[j - 1]++;
                } else if (ans[j] == 3) {
                    ans[j] = 1;
                    ans[j - 1]++;
                }
            }
            if (ans[27 - offset] == 2) {
                ans[0] = 0;
            } else if (ans[27 - offset] == 3) {
                ans[0] = 1;
            }
            if (ans[27 - offset] == 1) {
                ans[27 - offset - 1] = 1;
            } else {
                ans[27 - offset - 1] = 0;
            }
            offset++;
        }
        String out = "";
        for (int i = 0; i <= 53; i++) {
            out += ans[i];
        }

        return new DataType(out);
    }

    DataType remainderReg = new DataType("00000000000000000000000000000000");

    public boolean isZero(String s) {
        for (int i = 0; i < 32; i++) {
            if (s.charAt(i) == '1') {
                return false;
            }
        }
        return true;
    }

    /**
     * 返回两个二进制整数的除法结果
     * dest ÷ src
     *
     * @param src  32-bits
     * @param dest 32-bits
     * @return 32-bits
     */
    public DataType div(DataType src, DataType dest) {
        StringBuilder y = new StringBuilder(src.toString());
        StringBuilder s2 = new StringBuilder(dest.toString());
        remainderReg = new DataType(s2.toString());
        if (!isZero(y.toString())) {
            if (s2.charAt(0) == '1') {
                for (int i = 0; i <= 31; i++) {
                    s2.insert(0, "1");
                }
            } else {
                for (int i = 0; i <= 31; i++) {
                    s2.insert(0, "0");
                }
            }
            if (s2.charAt(0) != y.charAt(0)) {
                s2 = new StringBuilder(add(new DataType(s2.substring(0, 32)), new DataType(y.toString())).toString() + s2.substring(32, 64));
            } else {
                s2 = new StringBuilder(sub(new DataType(y.toString()), new DataType(s2.substring(0, 32))).toString() + s2.substring(32, 64));
            }
            for (int i = 0; i < 32; i++) {
                if (s2.charAt(0) == y.charAt(0)) { // 同号
                    s2.append('1');
                    s2.deleteCharAt(0);
                    s2 = new StringBuilder(sub(new DataType(y.toString()), new DataType(s2.substring(0, 32))).toString() + s2.substring(32, 64));
                } else {
                    s2.append('0');
                    s2.deleteCharAt(0);
                    s2 = new StringBuilder(add(new DataType(y.toString()), new DataType(s2.substring(0, 32))).toString() + s2.substring(32, 64));
                }
            }
            remainderReg = new DataType(s2.substring(0, 32));
            StringBuilder z = new StringBuilder(s2.substring(32, 64));
            if (Integer.parseInt(Transformer.binaryToInt(dest.toString())) == -Integer.parseInt(Transformer.binaryToInt(src.toString()))) {
                return new DataType(z.toString());
            }
            //以下为商修正
            z.deleteCharAt(0); // 左移
            if (remainderReg.toString().charAt(0) == y.charAt(0)) {
                z.append('1');
            } else {
                z.append('0');
            }
            if (dest.toString().charAt(0) != y.charAt(0)) { // 最低位加一
                int[] tmp = new int[32];
                for (int i = 0; i <= 31; i++) {
                    tmp[i] = z.charAt(i) - '0';
                }
                tmp[31]++;
                for (int i = 31; i > 0; i--) {
                    if (tmp[i] == 2) {
                        tmp[i - 1]++;
                        tmp[i] = 0;
                    }
                }
                if (tmp[0] == 2) {
                    tmp[0] = 0;
                }
                z = new StringBuilder();
                for (int i = 0; i < 32; i++) {
                    z.append(tmp[i]);
                }
            }

            // 以下为余数修正
            if (remainderReg.toString().charAt(0) != dest.toString().charAt(0)) {
                if (dest.toString().charAt(0) == y.charAt(0)) {
                    remainderReg = add(remainderReg, new DataType(y.toString()));
                } else {
                    remainderReg = sub(new DataType(y.toString()), remainderReg);
                }
            }
            if (Math.abs(Integer.parseInt(Transformer.binaryToInt(remainderReg.toString()))) == Math.abs(Integer.parseInt(Transformer.binaryToInt(y.toString())))) {
                if (remainderReg.toString().charAt(0) == y.charAt(0)) { // 余数和除数同号
                    if (z.toString().charAt(0) == '0') {
                        z = new StringBuilder(add(new DataType(Transformer.intToBinary(String.valueOf(1))), new DataType(z.toString())).toString());
                    } else {
                        z = new StringBuilder(sub(new DataType(Transformer.intToBinary(String.valueOf(1))), new DataType(z.toString())).toString());
                    }
                } else { // 余数和除数异号
                    if (z.toString().charAt(0) == '0') {
                        z = new StringBuilder(add(new DataType(Transformer.intToBinary(String.valueOf(1))), new DataType(z.toString())).toString());
                    } else {
                        z = new StringBuilder(sub(new DataType(Transformer.intToBinary(String.valueOf(1))), new DataType(z.toString())).toString());
                    }
                }
                remainderReg = new DataType("00000000000000000000000000000000");
            }
            return new DataType(z.toString());
        } else {
            throw new ArithmeticException();
        }
    }
}
