package com.d3.eth.withdrawal.withdrawalservice

/**
 * Events are emitted by withdrawal service
 */
sealed class WithdrawalServiceOutputEvent {

    /**
     * Refund in Ethereum chain
     * @param proof - proof given from all notaries for operation
     * @param isIrohaAnchored - is Iroha or Ethereum anchored token, determines strategy of
     * withdrawal
     */
    data class EthRefund(
        val proof: RollbackApproval,
        val isIrohaAnchored: Boolean
    ) : WithdrawalServiceOutputEvent()
}
