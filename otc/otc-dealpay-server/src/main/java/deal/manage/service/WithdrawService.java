package deal.manage.service;

import java.util.List;

import deal.manage.bean.Withdraw;

public interface WithdrawService {
	/**
	 * <p>代付订单生成</p>
	 * @param witb
	 * @return
	 */
	boolean addOrder(Withdraw witb);

	
	/**
	 * <p>根据代付订单号查询代付订单</p>
	 * @param orderId
	 * @return
	 */
	Withdraw findOrderId(String orderId);


	List<Withdraw> findWithdrawOrder(Withdraw bean);


	/**
	 * <p>修改订单为失败，说明原因</p>
	 * @param orderId				订单
	 * @param message				原因
	 * @return
	 */
	boolean updateStatusEr(String orderId, String message);


	/**
	 * <p>订单状态修改成功</p>
	 * @param orderId			订单号
	 * @param msg				修改消息
	 * @return
	 */
	boolean updateStatusSu(String orderId, String msg);


}
