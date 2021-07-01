package test.number;

import cn.hutool.core.thread.ThreadUtil;

import java.math.BigDecimal;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class charN {
	static int o = 1;
	static Lock lock = new ReentrantLock();

	public static void main(String[] args) {
		String amount = "";
		BigDecimal bigDecimal = new BigDecimal("20.000");
		String[] split = bigDecimal.toString().split("\\.");
		if (split.length == 1) {
			String s = bigDecimal.toString();
			s += ".0";
			split = s.split("\\.");
		}
		String startAmount = split[0];
		String endAmount = split[1];
		int length = endAmount.length();
		if (length == 1) {//当交易金额为整小数的时候        补充0
			endAmount += "0";
		} else if (endAmount.length() > 2) {
			endAmount = "00";
		}
		amount = startAmount + "." + endAmount;//得到正确的金额
		System.out.println(amount);


	}

	static boolean test() {
		lock.lock();
		try {
			o++;
			System.out.println("执行次数：" + o);
			int a = 1;
			if (o == 2) {
				System.out.println("执行次数2---------------------------：" + o);

			}
			int b = 5;


			int c = b - a;
			ThreadUtil.sleep(200);
			if (c == 4) {
				a = 2;

				return true;
			} else {
				System.out.println("错误，a == " + a);
				System.out.println("错误，c == " + c);
				return false;
			}
		} finally {
			lock.unlock();
		}


	}
}
