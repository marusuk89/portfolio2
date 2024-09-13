package com.itda.shop.store.api.v1.order;

import com.itda.core.business.exception.ConstraintException;
import com.itda.core.business.exception.ResourceNotFoundException;
import com.itda.core.business.exception.ServiceRuntimeException;
import com.itda.core.business.services.order.ShoppingCartService;
import com.itda.core.common.ErrorCode;
import com.itda.core.model.customer.Customer;
import com.itda.core.model.order.PurchasedOrder;
import com.itda.core.model.order.ShoppingCart;
import com.itda.shop.model.entity.CodeEntity;
import com.itda.shop.model.order.OrderConfirmation;
import com.itda.shop.model.order.PersistableOrder;
import com.itda.shop.model.order.PersistableShoppingCartItem;
import com.itda.shop.model.order.ReadableShoppingCart;
import com.itda.shop.model.product.PersistableBoolean;
import com.itda.shop.model.product.PersistableQuantity;
import com.itda.shop.store.facade.customer.CustomerFacade;
import com.itda.shop.store.facade.order.OrderFacade;
import com.itda.shop.store.facade.order.ShoppingCartFacade;
import com.itda.shop.utils.MarkdownDescription;
import com.itda.shop.utils.SwaggerMarkdown;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@MarkdownDescription(markdown = SwaggerMarkdown.SHOPPING_CART_DESCRIPTION)
@RequiredArgsConstructor
public class ShoppingCartApi
{
    private static final String DEFAULT_CODE = "default";

    private final ShoppingCartFacade shoppingCartFacade;

    private final ShoppingCartService shoppingCartService;

    private final CustomerFacade customerFacade;

    private final OrderFacade orderFacade;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/carts")
    @Operation(summary = "빈 장바구니 생성", hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "409", description = "고객의 장바구니가 이미 존재"),
            @ApiResponse(responseCode = "201", description = "장바구니 생성 성공", useReturnTypeSchema = true)})
    public CodeEntity createCart()
    {
        ShoppingCart newCart = shoppingCartService.getOrCreateShoppingCart(customerFacade.authenticatedUsername());

        return CodeEntity.with(newCart.getCode());
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/carts/default/items")
    @Operation(summary = "기본 장바구니에 상품을 추가")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "장바구니 상품 추가 성공")})
    public ReadableShoppingCart addToCart(
            @RequestBody List<@Valid PersistableShoppingCartItem> shoppingCartItems)
    {
        ShoppingCart cart = shoppingCartService.getOrCreateShoppingCart(customerFacade.authenticatedUsername());
        return shoppingCartFacade.addToCart(cart, shoppingCartItems);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/carts/instant/items")
    @Operation(summary = "즉시 구매 장바구니에 상품을 추가", description = "1회용 장바구니 생성")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "장바구니 상품 추가 성공")})
    public ReadableShoppingCart addToInstantCart(
            @RequestBody List<@Valid PersistableShoppingCartItem> shoppingCartItems)
    {
        ShoppingCart cart = shoppingCartService.createShoppingCart(customerFacade.authenticatedUsername(), true);
        return shoppingCartFacade.addToCart(cart, shoppingCartItems);
    }

    @PatchMapping(value = "/carts/{cart}/items/{item}/save-for-later")
    @Operation(summary = "장바구니에서 상품 추후 결제")
    @Parameters(value = {
            @Parameter(name = "cart", description = "장바구니 CODE", in = ParameterIn.PATH),
            @Parameter(name = "item", description = "장바구니 아이템 CODE", in = ParameterIn.PATH),
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "상품이 장바구니에 존재하지 않음"),
            @ApiResponse(responseCode = "200", description = "변경 후 업데이트 된 장바구니", useReturnTypeSchema = true)
    })
    public ReadableShoppingCart modifyCartSaveForLater(
            @PathVariable String cart,
            @PathVariable String item,
            @Valid @RequestBody PersistableBoolean select)
    {
        return shoppingCartFacade.saveForLater(getCart(customerFacade.authenticatedCustomer(), cart), item, select.isValue());
    }

    @PatchMapping(value = "/carts/{cart}/save-for-later")
    @Operation(summary = "장바구니에서 모든 상품 추후 결제 상태 변경")
    @Parameters(value = {
            @Parameter(name = "cart", description = "장바구니 CODE", in = ParameterIn.PATH),
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "장바구니가 존재하지 않음"),
            @ApiResponse(responseCode = "200", description = "변경 후 업데이트 된 장바구니", useReturnTypeSchema = true)
    })
    public ReadableShoppingCart modifyCartSaveForLaterByCart(
            @PathVariable String cart,
            @Valid @RequestBody PersistableBoolean select)
    {
        return shoppingCartFacade.saveForLaterByCart(getCart(customerFacade.authenticatedCustomer(), cart), select.isValue());
    }

    @PatchMapping(value = "/carts/{cart}/items/{item}/quantity")
    @Operation(summary = "장바구니에서 상품 수량 변경")
    @Parameters(value = {
            @Parameter(name = "cart", description = "장바구니 CODE", in = ParameterIn.PATH),
            @Parameter(name = "item", description = "장바구니 아이템 CODE", in = ParameterIn.PATH),
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "상품이 장바구니에 존재하지 않음"),
            @ApiResponse(responseCode = "200", description = "변경 후 업데이트 된 장바구니", useReturnTypeSchema = true)
    })
    public ReadableShoppingCart modifyCart(
            @PathVariable String cart,
            @PathVariable String item,
            @Valid @RequestBody PersistableQuantity quantity)
    {
        return shoppingCartFacade.modifyCart(getCart(customerFacade.authenticatedCustomer(), cart), item, quantity.getQuantity());
    }

    @PostMapping(value = "/carts/{code}/promo/{promo}")
    @Operation(hidden = true)
    public ResponseEntity<ReadableShoppingCart> modifyCart(@PathVariable String code,
            @PathVariable String promo, HttpServletResponse response)
    {
        try {
            ReadableShoppingCart cart = shoppingCartFacade.modifyCart(code, promo);

            if (cart == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(cart, HttpStatus.CREATED);
        }
        catch (Exception e) {
            if (e instanceof ResourceNotFoundException) {
                throw (ResourceNotFoundException) e;
            }
            else {
                throw new ServiceRuntimeException(e);
            }
        }
    }

    @PostMapping(value = "/carts/{code}/multi", consumes = {"application/json"}, produces = {"application/json"})
    @Operation(hidden = true)
    public ResponseEntity<ReadableShoppingCart> modifyCart(@PathVariable String code, @Valid @RequestBody PersistableShoppingCartItem[] shoppingCartItems)
    {
        try {
            ReadableShoppingCart cart = shoppingCartFacade.modifyCartMulti(code, Arrays.asList(shoppingCartItems));

            return new ResponseEntity<>(cart, HttpStatus.CREATED);
        }
        catch (Exception e) {
            if (e instanceof ResourceNotFoundException) {
                throw (ResourceNotFoundException) e;
            }
            else {
                throw new ServiceRuntimeException(e);
            }
        }
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/carts/{code}")
    @Parameters(value = {
            @Parameter(name = "code", description = "장바구니 code", in = ParameterIn.PATH),
    })
    @Operation(summary = "장바구니 조회", description = "필요시 빈 장바구니 생성")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "장바구니가 존재하지 않음"),
            @ApiResponse(responseCode = "200", description = "기본 혹은 빈 장바구니", useReturnTypeSchema = true)})
    public ReadableShoppingCart getByCode(@PathVariable String code)
    {
        Customer customer = customerFacade.authenticatedCustomer();

        return shoppingCartFacade.reloadShoppingCart(getCart(customer, code));
    }

    @DeleteMapping(value = "/carts/{cart}/items/{item}")
    @Operation(summary = "장바구니에서 상품을 제거")
    @Parameters(value = {
            @Parameter(name = "cart", description = "장바구니 CODE", in = ParameterIn.PATH),
            @Parameter(name = "item", description = "장바구니 아이템 CODE", in = ParameterIn.PATH),
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "상품이 장바구니에 존재하지 않음"),
            @ApiResponse(responseCode = "200", description = "상품 제거 후 업데이트 된 장바구니", useReturnTypeSchema = true)
    })
    public ReadableShoppingCart deleteCartItem(
            @PathVariable String cart,
            @PathVariable String item)
    {
        return shoppingCartFacade.removeShoppingCartItem(getCart(customerFacade.authenticatedCustomer(), cart), item);
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping(value = "/carts/{cart}/checkout")
    @Parameters(value = {
            @Parameter(name = "cart", description = "장바구니 code", in = ParameterIn.PATH),
    })
    @Operation(summary = "장바구니 결제 시작", description = "즉시 구매에 사용")
    public OrderConfirmation checkout(
            @PathVariable String cart,
            @Valid @RequestBody PersistableOrder order)
    {
        Customer customer = customerFacade.authenticatedCustomer();

        return checkout(customer, getCart(customer, cart), order);
    }

    private ShoppingCart getCart(Customer customer, String code)
    {
        if (DEFAULT_CODE.equals(code)) {
            return shoppingCartService.getOrCreateShoppingCart(customer.getUsername());
        }
        else {
            return shoppingCartService.findByCode(code)
                    .orElseThrow(() -> ResourceNotFoundException.with("Cart", code));
        }
    }

    private OrderConfirmation checkout(Customer customer, ShoppingCart cart, PersistableOrder order)
    {
        if (cart.getPurchasedOrderCode() != null) {
            throw new ConstraintException(ErrorCode.CART_ALREADY_CHECKED_OUT, "Cart " + cart.getCode());
        }
        order.setShoppingCart(cart);
        order.setCustomer(customer);

        PurchasedOrder modelOrder = orderFacade.processOrder(order, customer);

        return orderFacade.orderConfirmation(modelOrder, customer);
    }
}
