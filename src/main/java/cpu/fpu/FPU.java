package cpu.fpu;

import cpu.alu.ALU;
import util.DataType;
import util.IEEE754Float;
import util.Transformer;

/**
 * floating point unit
 * 执行浮点运算的抽象单元
 * 浮点数精度：使用3位保护位进行计算
 */
public class FPU {

    private final String[][] addCorner = new String[][]{
            {IEEE754Float.P_ZERO, IEEE754Float.P_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.N_ZERO, IEEE754Float.P_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.P_ZERO, IEEE754Float.N_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.N_ZERO, IEEE754Float.N_ZERO, IEEE754Float.N_ZERO},
            {IEEE754Float.P_INF, IEEE754Float.N_INF, IEEE754Float.NaN},
            {IEEE754Float.N_INF, IEEE754Float.P_INF, IEEE754Float.NaN}
    };

    private final String[][] subCorner = new String[][]{
            {IEEE754Float.P_ZERO, IEEE754Float.P_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.N_ZERO, IEEE754Float.P_ZERO, IEEE754Float.N_ZERO},
            {IEEE754Float.P_ZERO, IEEE754Float.N_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.N_ZERO, IEEE754Float.N_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.P_INF, IEEE754Float.P_INF, IEEE754Float.NaN},
            {IEEE754Float.N_INF, IEEE754Float.N_INF, IEEE754Float.NaN}
    };

    private final String[][] mulCorner = new String[][]{
            {IEEE754Float.P_ZERO, IEEE754Float.N_ZERO, IEEE754Float.N_ZERO},
            {IEEE754Float.N_ZERO, IEEE754Float.P_ZERO, IEEE754Float.N_ZERO},
            {IEEE754Float.P_ZERO, IEEE754Float.P_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.N_ZERO, IEEE754Float.N_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.P_ZERO, IEEE754Float.P_INF, IEEE754Float.NaN},
            {IEEE754Float.P_ZERO, IEEE754Float.N_INF, IEEE754Float.NaN},
            {IEEE754Float.N_ZERO, IEEE754Float.P_INF, IEEE754Float.NaN},
            {IEEE754Float.N_ZERO, IEEE754Float.N_INF, IEEE754Float.NaN},
            {IEEE754Float.P_INF, IEEE754Float.P_ZERO, IEEE754Float.NaN},
            {IEEE754Float.P_INF, IEEE754Float.N_ZERO, IEEE754Float.NaN},
            {IEEE754Float.N_INF, IEEE754Float.P_ZERO, IEEE754Float.NaN},
            {IEEE754Float.N_INF, IEEE754Float.N_ZERO, IEEE754Float.NaN}
    };

    private final String[][] divCorner = new String[][]{
            {IEEE754Float.P_ZERO, IEEE754Float.P_ZERO, IEEE754Float.NaN},
            {IEEE754Float.N_ZERO, IEEE754Float.N_ZERO, IEEE754Float.NaN},
            {IEEE754Float.P_ZERO, IEEE754Float.N_ZERO, IEEE754Float.NaN},
            {IEEE754Float.N_ZERO, IEEE754Float.P_ZERO, IEEE754Float.NaN},
            {IEEE754Float.P_INF, IEEE754Float.P_INF, IEEE754Float.NaN},
            {IEEE754Float.N_INF, IEEE754Float.N_INF, IEEE754Float.NaN},
            {IEEE754Float.P_INF, IEEE754Float.N_INF, IEEE754Float.NaN},
            {IEEE754Float.N_INF, IEEE754Float.P_INF, IEEE754Float.NaN},
    };
    ALU alu = new ALU();

    /**
     * compute the float add of (dest + src)
     */
    public DataType add(DataType src, DataType dest) {
        String a = dest.toString(); // 首先排除是NaN的情况
        String b = src.toString();
        if (a.matches(IEEE754Float.NaN_Regular) || b.matches(IEEE754Float.NaN_Regular)) {
            return new DataType(IEEE754Float.NaN);
        }
        String tmpAns = cornerCheck(addCorner, src.toString(), dest.toString()); // 然后排除0和Inf的情况
        if (tmpAns != null) {
            return new DataType(tmpAns);
        }
        String n1 = src.toString();
        String n2 = dest.toString();

        if (n1.equals(IEEE754Float.N_ZERO) || n1.equals(IEEE754Float.P_ZERO)) {
            return new DataType(n2);
        }
        if (n2.equals(IEEE754Float.N_ZERO) || n2.equals(IEEE754Float.P_ZERO)) {
            return new DataType(n1);
        }

        char sign1 = n1.charAt(0); // 符号
        char sign2 = n2.charAt(0);
        String exp1 = n1.substring(1, 9); // 阶码
        String exp2 = n2.substring(1, 9);
        String tail1 = n1.substring(9, 32); // 尾数
        String tail2 = n2.substring(9, 32);

        if (exp1.equals("11111111")) { // 其中一个数是+/-Inf
            return new DataType(n1);
        } else if (exp2.equals("11111111")) {
            return new DataType(n2);
        }

        if (exp1.equals("00000000")) { // 处理非规格数
            exp1 = "00000001";
            tail1 = "0" + tail1 + "000";
        } else {
            tail1 = "1" + tail1 + "000";
        }
        if (exp2.equals("00000000")) {
            exp2 = "00000001";
            tail2 = "0" + tail2 + "000";
        } else {
            tail2 = "1" + tail2 + "000";
        }

        // 以下是对阶的过程
        int cnt = 0; // 移动的位数
        int op = 0; // 对谁进行移动
        String ansExp = ""; // 最后的阶码
        char ansSign = 0;

        if (exp1.equals(exp2)) { // 不需要对阶
            ansExp = exp1;
            if (tail1.equals(tail2)) {
                if (sign1 != sign2) {
                    return new DataType("00000000000000000000000000000000");
                } else {
                    ansSign = sign1;
                }
            } else {
                for (int i = 0; ;i++) {
                    if (tail1.charAt(i) != tail2.charAt(i)) {
                        if (tail1.charAt(i) == '1') {
                            ansSign = sign1;
                            op = 2;
                        } else {
                            ansSign = sign2;
                            op = 1;
                        }
                        break;
                    }
                }
            }
        } else {
            for (int i = 0; i < 9; i++) {
                if (exp1.charAt(i) == exp2.charAt(i)) {
                    continue;
                } else if (exp1.charAt(i) == '1') { // n1 比较大
                    op = 2;
                    ansSign = sign1;
                    ansExp = exp1;
                    String tmp = alu.sub(new DataType(exp2 + "000000000000000000000000"), new DataType(exp1 + "000000000000000000000000")).toString().substring(0, 8);
                    cnt = Integer.parseInt(Transformer.binaryToInt(tmp));
                    break;
                } else if (exp2.charAt(i) == '1') {
                    op = 1;
                    ansSign = sign2;
                    ansExp = exp2;
                    String tmp = alu.sub(new DataType(exp1 + "000000000000000000000000"), new DataType(exp2 + "000000000000000000000000")).toString().substring(0, 8);
                    cnt = Integer.parseInt(Transformer.binaryToInt(tmp));
                    break;
                }
            }
            if (op == 1) { // 移位
                tail1 = rightShift(tail1, cnt);
            } else {
                tail2 = rightShift(tail2, cnt);
            }
        }

        tail1 = "0" + tail1 + "0000";
        tail2 = "0" + tail2 + "0000";
        String ansTail = "";
        if (sign1 == sign2) {
            ansTail = alu.add(new DataType(tail1), new DataType(tail2)).toString().substring(0, 28);
        } else {
            if (op == 1) {
                ansTail = alu.sub(new DataType(tail1), new DataType(tail2)).toString().substring(0, 28);
            } else {
                ansTail = alu.sub(new DataType(tail2), new DataType(tail1)).toString().substring(0, 28);
            }
        }
        if (ansTail.charAt(0) == '1') {
            ansTail = '1' + rightShift(ansTail.substring(1, 28), 1).substring(1, 27);
            ansExp = alu.add(new DataType(ansExp + "000000000000000000000000"), new DataType("00000001" + "000000000000000000000000")).toString().substring(0, 8);
        } else {
            ansTail = ansTail.substring(1, 28);
            int n = 0;
            int t = Integer.parseInt(Transformer.binaryToInt(ansExp));
            while (ansTail.charAt(0) == '0' && n < t) {
                n++;
                ansTail = ansTail.substring(1, 27) + '0';
            }
            t -= n;
            ansExp = Transformer.intToBinary(String.valueOf(t)).substring(24, 32);
            if (ansExp.equals("00000000")) {
                ansTail = '0' + ansTail.substring(0, 26);
            }
        }

        return new DataType(round(ansSign, ansExp, ansTail));
    }

    /**
     * compute the float add of (dest - src)
     */
    public DataType sub(DataType src, DataType dest) {
        String a = dest.toString(); // 首先排除是NaN的情况
        String b = src.toString();
        if (a.matches(IEEE754Float.NaN_Regular) || b.matches(IEEE754Float.NaN_Regular)) {
            return new DataType(IEEE754Float.NaN);
        }
        String tmpAns = cornerCheck(subCorner, src.toString(), dest.toString()); // 然后排除0和Inf的情况
        if (tmpAns != null) {
            return new DataType(tmpAns);
        }
        b = (b.charAt(0) == '1' ? '0' : '1') + b.substring(1,32);
        return this.add(new DataType(a), new DataType(b));
    }


    public boolean judgeTail (String str) {
        for (int i = 0; i < 27; i++) {
            if (str.charAt(i) == '1') {
                return true;
            }
        }
        return false;
    }
    /**
     * compute the float mul of (dest * src)
     */
    public DataType mul(DataType src,DataType dest) {
        String a = dest.toString(); // 首先排除是NaN的情况
        String b = src.toString();
        if (a.matches(IEEE754Float.NaN_Regular) || b.matches(IEEE754Float.NaN_Regular)) {
            return new DataType(IEEE754Float.NaN);
        }
        String tmpAns = cornerCheck(mulCorner, src.toString(), dest.toString()); // 然后排除0和Inf的情况
        if (tmpAns != null) {
            return new DataType(tmpAns);
        }

        String n1 = src.toString();
        String n2 = dest.toString();

        char sign1 = n1.charAt(0); // 符号
        char sign2 = n2.charAt(0);
        String exp1 = n1.substring(1, 9); // 阶码
        String exp2 = n2.substring(1, 9);
        String tail1 = n1.substring(9, 32); // 尾数
        String tail2 = n2.substring(9, 32);

        char ansSign = sign1 == sign2 ? '0' : '1';

        if (exp1.equals("11111111")) { // 其中一个数是+/-Inf
            return new DataType(ansSign + n1.substring(1, 32));
        } else if (exp2.equals("11111111")) {
            return new DataType(ansSign + n2.substring(1, 32));
        }

        if (exp1.equals("00000000")) { // 处理非规格数
            exp1 = "00000001";
            tail1 = "0" + tail1 + "000";
        } else {
            tail1 = "1" + tail1 + "000";
        }
        if (exp2.equals("00000000")) {
            exp2 = "00000001";
            tail2 = "0" + tail2 + "000";
        } else {
            tail2 = "1" + tail2 + "000";
        }
        String ansExp = "";
        int n = Integer.parseInt(Transformer.binaryToInt(exp1)) + Integer.parseInt(Transformer.binaryToInt(exp2)) - 126;

        String ansTail = "";
        int[] N1 = new int[27];
        int[] ans = new int[54];
        for (int i = 0; i < 27; i++) {
            N1[i] = tail1.charAt(i) - '0';
        }
        int offset = 27;
        for (int i = 26; i >= 0; i--) {
            if (tail2.charAt(i) == '1') {
                for (int j = offset, k = 0; j < offset + 27; j++, k++) {
                    ans[j] += N1[k];
                }
                for (int j = offset + 26; j > 0; j--) {
                    if (ans[j] == 2) {
                        ans[j] = 0;
                        ans[j - 1]++;
                    } else if (ans[j] == 3) {
                        ans[j] = 1;
                        ans[j - 1]++;
                    }
                }
                if (ans[0] == 2) {
                    ans[0] = 0;
                }
            }
            offset--;
        }
        for (int i = 0; i < 54; i++) {
            ansTail += ans[i];
        }

        while (ansTail.charAt(0) == '0' && n > 0) {
            ansTail = ansTail.substring(1, 54) + '0';
            n--;
        }
        while (judgeTail(ansTail.substring(0, 27)) && n < 0) {
            ansTail = rightShift(ansTail, 1);
            n++;
        }

        if (n >= 255) {
            return new DataType(ansSign + IEEE754Float.P_INF.substring(1, 32));
        } else if (n < 0) {
            return new DataType(ansSign + IEEE754Float.P_ZERO.substring(1, 32));
        } else if (n == 0) {
            ansTail = rightShift(ansTail, 1);
        }
        ansExp = Transformer.intToBinary(String.valueOf(n)).substring(24, 32);

        return new DataType(round(ansSign, ansExp, ansTail));
    }
    boolean judge (int[] a, int[] b, int n) {
        for (int i = 0; i < 28; i++) {
            if (a[i + n] < b[i]) {
                return false;
            } else if (a[i + n] > b[i]) {
                return true;
            }
        }
        return true;
    }

    /**
     * compute the float mul of (dest / src)
     */
    public DataType div(DataType src,DataType dest) {
        String a = dest.toString(); // 首先排除是NaN的情况
        String b = src.toString();
        if (a.matches(IEEE754Float.NaN_Regular) || b.matches(IEEE754Float.NaN_Regular)) {
            return new DataType(IEEE754Float.NaN);
        }
        String tmpAns = cornerCheck(divCorner, src.toString(), dest.toString()); // 然后排除0和Inf的情况
        if (tmpAns != null) {
            return new DataType(tmpAns);
        }
        if (b.equals(IEEE754Float.N_ZERO) || b.equals(IEEE754Float.P_ZERO)) {
            throw new ArithmeticException();
        }

        String n1 = src.toString();
        String n2 = dest.toString();

        char sign1 = n1.charAt(0); // 符号
        char sign2 = n2.charAt(0);
        String exp1 = n1.substring(1, 9); // 阶码
        String exp2 = n2.substring(1, 9);
        String tail1 = n1.substring(9, 32); // 尾数
        StringBuilder tail2 = new StringBuilder(n2.substring(9, 32));

        char ansSign = sign1 == sign2 ? '0' : '1';

        if (exp1.equals("11111111")) { // 其中一个数是+/-Inf
            return new DataType(ansSign + n1.substring(1, 32));
        } else if (exp2.equals("11111111")) {
            return new DataType(ansSign + n2.substring(1, 32));
        }

        if (exp1.equals("00000000")) { // 处理非规格数
            exp1 = "00000001";
            tail1 = "0" + tail1 + "000";
        } else {
            tail1 = "1" + tail1 + "000";
        }
        if (exp2.equals("00000000")) {
            exp2 = "00000001";
            tail2 = new StringBuilder("0" + tail2 + "000");
        } else {
            tail2 = new StringBuilder("1" + tail2 + "000");
        }

        int[] N1 = new int[28];
        int[] Nc = new int[28];
        int[] N2 = new int[55];
        Nc[0] = 1;
        for (int i = 0; i < 27; i++) {
            N1[i + 1] = tail1.charAt(i) - '0';
            Nc[i + 1] = 1 - N1[i + 1];
            N2[i + 1] = tail2.charAt(i) - '0';
        }
        Nc[27]++;
        for (int i = 27; i > 0; i--) {
            if (Nc[i] == 2) {
                Nc[i] = 0;
                Nc[i - 1]++;
            }
        }
        if (Nc[0] == 2) {
            Nc[0] = 0;
        }

        StringBuilder ansTail = new StringBuilder();
        for (int i = 0; i < 27; i++) {
            if (judge(N2, N1, i)) {
                ansTail.append("1");
                for (int j = i, k = 0; j < i + 28; j++, k++) {
                    N2[j] += Nc[k];
                }
                for (int j = i + 28; j > 0; j--) {
                    if (N2[j] == 2) {
                        N2[j] = 0;
                        N2[j - 1]++;
                    } else if (N2[j] == 3) {
                        N2[j] = 1;
                        N2[j - 1]++;
                    }
                }
                if (N2[0] == 2) {
                    N2[0] = 0;
                } else if (N2[0] == 3) {
                    N2[0] = 1;
                }
            } else {
                ansTail.append('0');
            }
        }
        String ansExp = "";
        int n = Integer.parseInt(Transformer.binaryToInt(exp2)) - Integer.parseInt(Transformer.binaryToInt(exp1)) + 127;
        while (ansTail.charAt(0) == '0' && n > 0) {
            ansTail = new StringBuilder(ansTail.substring(1, 27) + '0');
            n--;
        }
        while (judgeTail(ansTail.toString().substring(0, 27)) && n < 0) {
            ansTail = new StringBuilder(rightShift(ansTail.toString(), 1));
            n++;
        }
        if (n >= 255) {
            return new DataType(ansSign + IEEE754Float.P_INF.substring(1, 32));
        } else if (n < 0) {
            return new DataType(ansSign + IEEE754Float.P_ZERO.substring(1, 32));
        } else if (n == 0) {
            ansTail = new StringBuilder(rightShift(ansTail.toString(), 1));
        }

        ansExp = Transformer.intToBinary(String.valueOf(n)).substring(24, 32);

        return new DataType(round(ansSign, ansExp, ansTail.toString()));
    }
    /**
     * check corner cases of mul and div
     *
     * @param cornerMatrix corner cases pre-stored
     * @param oprA first operand (String)
     * @param oprB second operand (String)
     * @return the result of the corner case (String)
     */
    private String cornerCheck(String[][] cornerMatrix, String oprA, String oprB) {
        for (String[] matrix : cornerMatrix) {
            if (oprA.equals(matrix[0]) && oprB.equals(matrix[1])) {
                return matrix[2];
            }
        }
        return null;
    }

    /**
     * right shift a num without considering its sign using its string format
     *
     * @param operand to be moved
     * @param n       moving nums of bits
     * @return after moving
     */
    private String rightShift(String operand, int n) {
        StringBuilder result = new StringBuilder(operand);  //保证位数不变
        boolean sticky = false;
        for (int i = 0; i < n; i++) {
            sticky = sticky || result.toString().endsWith("1");
            result.insert(0, "0");
            result.deleteCharAt(result.length() - 1);
        }
        if (sticky) {
            result.replace(operand.length() - 1, operand.length(), "1");
        }
        return result.substring(0, operand.length());
    }

    /**
     * 对GRS保护位进行舍入
     *
     * @param sign    符号位
     * @param exp     阶码
     * @param sig_grs 带隐藏位和保护位的尾数
     * @return 舍入后的结果
     */
    private String round(char sign, String exp, String sig_grs) {
        int grs = Integer.parseInt(sig_grs.substring(24, 27), 2);
        if ((sig_grs.substring(27).contains("1")) && (grs % 2 == 0)) {
            grs++;
        }
        String sig = sig_grs.substring(0, 24); // 隐藏位+23位
        if (grs > 4) {
            sig = oneAdder(sig);
        } else if (grs == 4 && sig.endsWith("1")) {
            sig = oneAdder(sig);
        }

        if (Integer.parseInt(sig.substring(0, sig.length() - 23), 2) > 1) {
            sig = rightShift(sig, 1);
            exp = oneAdder(exp).substring(1);
        }
        if (exp.equals("11111111")) {
            return sign == '0' ? IEEE754Float.P_INF : IEEE754Float.N_INF;
        }

        return sign + exp + sig.substring(sig.length() - 23);
    }

    /**
     * add one to the operand
     *
     * @param operand the operand
     * @return result after adding, the first position means overflow (not equal to the carry to the next)
     *         and the remains means the result
     */
    private String oneAdder(String operand) {
        int len = operand.length();
        StringBuilder temp = new StringBuilder(operand);
        temp.reverse();
        int[] num = new int[len];
        for (int i = 0; i < len; i++) num[i] = temp.charAt(i) - '0';  //先转化为反转后对应的int数组
        int bit = 0x0;
        int carry = 0x1;
        char[] res = new char[len];
        for (int i = 0; i < len; i++) {
            bit = num[i] ^ carry;
            carry = num[i] & carry;
            res[i] = (char) ('0' + bit);  //显示转化为char
        }
        String result = new StringBuffer(new String(res)).reverse().toString();
        return "" + (result.charAt(0) == operand.charAt(0) ? '0' : '1') + result;  //注意有进位不等于溢出，溢出要另外判断
    }

}
