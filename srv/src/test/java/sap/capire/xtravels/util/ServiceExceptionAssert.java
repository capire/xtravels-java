package sap.capire.xtravels.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.assertj.core.api.ThrowableAssertAlternative;
import org.assertj.core.api.ThrowableTypeAssert;

public class ServiceExceptionAssert extends ThrowableTypeAssert<ServiceException> {

  private ServiceExceptionAssert() {
    super(ServiceException.class);
  }

  public static ServiceExceptionAssert assertThatServiceException() {
    return new ServiceExceptionAssert();
  }

  @Override
  public ServiceExceptionAssertAlternative isThrownBy(ThrowingCallable throwingCallable) {
    return (ServiceExceptionAssertAlternative) super.isThrownBy(throwingCallable);
  }

  @Override
  protected ThrowableAssertAlternative<ServiceException> buildThrowableTypeAssert(
      ServiceException throwable) {
    return new ServiceExceptionAssertAlternative(throwable);
  }

  public static class ServiceExceptionAssertAlternative
      extends ThrowableAssertAlternative<ServiceException> {

    public ServiceExceptionAssertAlternative(ServiceException actual) {
      super(actual);
    }

    public ServiceExceptionAssertAlternative withLocalizedMessage(String localizedMessage) {
      assertThat(actual.getLocalizedMessage()).isEqualTo(localizedMessage);

      return this;
    }

    public ServiceExceptionAssertAlternative isBadRequest() {
      assertThat(actual.getErrorStatus().getHttpStatus())
          .isEqualTo(ErrorStatuses.BAD_REQUEST.getHttpStatus());

      return this;
    }

    public ServiceExceptionAssertAlternative withMessageOrKey(String messageOrKey) {
      assertThat(actual.getMessageLookup().getMessageOrKey()).isEqualTo(messageOrKey);

      return this;
    }

    public ServiceExceptionAssertAlternative thatTargets(String target, String... additional) {
      assertThat(actual.getMessageTarget().getRef().path()).isEqualTo(target);
      assertThat(actual.getAdditionalTargets().size()).isEqualTo(additional.length);
      for (int i = 0; i < additional.length; i++) {
        assertThat(actual.getAdditionalTargets().get(i).getRef().path()).isEqualTo(additional[i]);
      }

      return this;
    }
  }
}
